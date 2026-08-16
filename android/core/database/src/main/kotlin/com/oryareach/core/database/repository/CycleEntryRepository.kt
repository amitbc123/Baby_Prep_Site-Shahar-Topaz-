package com.oryareach.core.database.repository

import androidx.room.withTransaction
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.database.SearchIndexer
import com.oryareach.core.database.entity.CycleEntryEntity
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.mapper.toCycleEntry
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.FlowLevel
import com.oryareach.core.model.Mood
import com.oryareach.core.model.PainLevel
import com.oryareach.core.model.Symptom
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import java.util.UUID

/**
 * The write path for a day's logged detail (flow/symptoms/mood/pain/note). At most one entry
 * per calendar date: logging a date that already has an entry updates it in place rather than
 * creating a second row, found by date lookup rather than a DB constraint (see
 * [com.oryareach.core.database.entity.CycleEntryEntity]'s index comment for why).
 */
class CycleEntryRepository(
    private val database: OrYareachDatabase,
    private val syncTrigger: SyncTrigger,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val entries get() = database.cycleEntryDao()
    private val operations get() = database.syncOperationDao()
    private val search = SearchIndexer(database)

    fun observeInRange(workspaceId: String, start: LocalDate, end: LocalDate) =
        entries.observeInRange(workspaceId, start.toString(), end.toString())
            .map { list -> list.map { it.toCycleEntry() } }

    suspend fun logEntry(
        workspaceId: String,
        userId: String,
        date: LocalDate,
        flow: FlowLevel?,
        symptoms: List<Symptom>,
        mood: List<Mood>,
        pain: PainLevel?,
        note: String?,
    ) {
        val existing = entries.findByDate(workspaceId, date.toString())
        val timestamp = now()

        val entity = if (existing != null) {
            existing.copy(
                flow = flow,
                symptoms = symptoms,
                mood = mood,
                pain = pain,
                note = note,
                sync = existing.sync.copy(
                    updatedAt = timestamp,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    clientMutationId = newId(),
                ),
            )
        } else {
            CycleEntryEntity(
                id = newId(),
                date = date.toString(),
                flow = flow,
                symptoms = symptoms,
                mood = mood,
                pain = pain,
                note = note,
                sync = SyncMetaEntity(
                    workspaceId = workspaceId,
                    createdBy = userId,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    clientMutationId = newId(),
                ),
            )
        }

        val operation = if (existing != null) SyncOperationType.UPDATE else SyncOperationType.CREATE
        database.withTransaction {
            entries.upsert(entity)
            search.index(EntityType.CYCLE_ENTRY, entity.id, entity.sync.workspaceId, "", entity.note.orEmpty())
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = entity.id,
                    entityType = EntityType.CYCLE_ENTRY,
                    operation = operation,
                    clientMutationId = entity.sync.clientMutationId.orEmpty(),
                    createdAt = timestamp,
                ),
            )
            operations.removeSuperseded(entity.id, opId)
        }
        syncTrigger.syncNow()
    }

    suspend fun delete(id: String) {
        val timestamp = now()
        database.withTransaction {
            entries.softDelete(id, timestamp)
            search.remove(id)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = id,
                    entityType = EntityType.CYCLE_ENTRY,
                    operation = SyncOperationType.DELETE,
                    clientMutationId = newId(),
                    createdAt = timestamp,
                ),
            )
            operations.removeSuperseded(id, opId)
        }
        syncTrigger.syncNow()
    }
}
