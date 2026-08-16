package com.oryareach.core.database.repository

import androidx.room.withTransaction
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.database.SearchIndexer
import com.oryareach.core.database.entity.MenstrualCycleEntity
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.mapper.toCycle
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.MenstrualCycle
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import java.util.UUID

/**
 * The write path for logged periods. Mirrors [TaskRepository]'s outbox pattern: only the
 * facts the user entered are ever stored — length, predictions and fertile-window estimates
 * are computed at read time in `:core:domain`, never persisted here.
 */
class CycleRepository(
    private val database: OrYareachDatabase,
    private val syncTrigger: SyncTrigger,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val cycles get() = database.menstrualCycleDao()
    private val operations get() = database.syncOperationDao()
    private val search = SearchIndexer(database)

    fun observeAll(workspaceId: String): Flow<List<MenstrualCycle>> =
        cycles.observeAll(workspaceId).map { list -> list.map { it.toCycle() } }

    fun observeOngoing(workspaceId: String): Flow<MenstrualCycle?> =
        cycles.observeOngoing(workspaceId).map { it?.toCycle() }

    suspend fun startPeriod(
        workspaceId: String,
        userId: String,
        startDate: LocalDate,
        note: String? = null,
    ) {
        val timestamp = now()
        val entity = MenstrualCycleEntity(
            id = newId(),
            startDate = startDate.toString(),
            endDate = null,
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
        enqueue(entity, SyncOperationType.CREATE)
    }

    suspend fun endPeriod(id: String, endDate: LocalDate) {
        val existing = cycles.findById(id) ?: return
        val updated = existing.copy(endDate = endDate.toString())
        enqueue(withBumpedSync(updated), SyncOperationType.UPDATE)
    }

    suspend fun updateNote(id: String, note: String?) {
        val existing = cycles.findById(id) ?: return
        enqueue(withBumpedSync(existing.copy(note = note)), SyncOperationType.UPDATE)
    }

    suspend fun delete(id: String) {
        val timestamp = now()
        database.withTransaction {
            cycles.softDelete(id, timestamp)
            search.remove(id)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = id,
                    entityType = EntityType.CYCLE,
                    operation = SyncOperationType.DELETE,
                    clientMutationId = newId(),
                    createdAt = timestamp,
                ),
            )
            operations.removeSuperseded(id, opId)
        }
        syncTrigger.syncNow()
    }

    private fun withBumpedSync(entity: MenstrualCycleEntity): MenstrualCycleEntity = entity.copy(
        sync = entity.sync.copy(
            updatedAt = now(),
            syncStatus = SyncStatus.PENDING_UPDATE,
            clientMutationId = newId(),
        ),
    )

    private suspend fun enqueue(entity: MenstrualCycleEntity, operation: SyncOperationType) {
        database.withTransaction {
            cycles.upsert(entity)
            search.index(EntityType.CYCLE, entity.id, entity.sync.workspaceId, "", entity.note.orEmpty())
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = entity.id,
                    entityType = EntityType.CYCLE,
                    operation = operation,
                    clientMutationId = entity.sync.clientMutationId ?: newId(),
                    createdAt = now(),
                ),
            )
            operations.removeSuperseded(entity.id, opId)
        }
        syncTrigger.syncNow()
    }
}
