package com.oryareach.core.database.repository

import androidx.room.withTransaction
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.database.SearchIndexer
import com.oryareach.core.database.entity.ImportantDateEntity
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.mapper.toImportantDate
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.ImportantDate
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import java.util.UUID

/** The write path for important dates — same outbox pattern as [TaskRepository]. */
class ImportantDateRepository(
    private val database: OrYareachDatabase,
    private val syncTrigger: SyncTrigger,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dates get() = database.importantDateDao()
    private val operations get() = database.syncOperationDao()
    private val search = SearchIndexer(database)

    fun observeAll(workspaceId: String): Flow<List<ImportantDate>> =
        dates.observeAll(workspaceId).map { list -> list.map { it.toImportantDate() } }

    suspend fun create(workspaceId: String, userId: String, date: LocalDate, title: String, wish: String? = null) {
        val timestamp = now()
        val entity = ImportantDateEntity(
            id = newId(),
            date = date.toString(),
            title = title,
            wish = wish,
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

    suspend fun update(id: String, date: LocalDate, title: String, wish: String?) {
        val existing = dates.findById(id) ?: return
        val updated = existing.copy(date = date.toString(), title = title, wish = wish)
        enqueue(withBumpedSync(updated), SyncOperationType.UPDATE)
    }

    suspend fun delete(id: String) {
        val timestamp = now()
        database.withTransaction {
            dates.softDelete(id, timestamp)
            search.remove(id)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = id,
                    entityType = EntityType.IMPORTANT_DATE,
                    operation = SyncOperationType.DELETE,
                    clientMutationId = newId(),
                    createdAt = timestamp,
                ),
            )
            operations.removeSuperseded(id, opId)
        }
        syncTrigger.syncNow()
    }

    private fun withBumpedSync(entity: ImportantDateEntity): ImportantDateEntity = entity.copy(
        sync = entity.sync.copy(
            updatedAt = now(),
            syncStatus = SyncStatus.PENDING_UPDATE,
            clientMutationId = newId(),
        ),
    )

    private suspend fun enqueue(entity: ImportantDateEntity, operation: SyncOperationType) {
        database.withTransaction {
            dates.upsert(entity)
            search.index(EntityType.IMPORTANT_DATE, entity.id, entity.sync.workspaceId, entity.title, entity.wish.orEmpty())
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = entity.id,
                    entityType = EntityType.IMPORTANT_DATE,
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
