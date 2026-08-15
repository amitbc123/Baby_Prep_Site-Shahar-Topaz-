package com.oryareach.core.database.repository

import androidx.room.withTransaction
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.database.entity.FolderEntity
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.mapper.toFolder
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.Folder
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * The write path for folders. [path] is derived here from the parent's stored path, never
 * accepted from a caller, so it can never drift from [parentId].
 */
class FolderRepository(
    private val database: OrYareachDatabase,
    private val syncTrigger: SyncTrigger,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val folders get() = database.folderDao()
    private val operations get() = database.syncOperationDao()

    fun observeChildren(workspaceId: String, parentId: String?): Flow<List<Folder>> =
        folders.observeChildren(workspaceId, parentId).map { list -> list.map { it.toFolder() } }

    suspend fun create(workspaceId: String, userId: String, name: String, parentId: String?): Folder {
        val timestamp = now()
        val id = newId()
        val parentPath = parentId?.let { folders.findById(it)?.path }.orEmpty()
        val entity = FolderEntity(
            id = id,
            name = name,
            parentId = parentId,
            path = "$parentPath$id/",
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
        return entity.toFolder()
    }

    suspend fun rename(id: String, name: String) {
        val existing = folders.findById(id) ?: return
        enqueue(withBumpedSync(existing.copy(name = name)), SyncOperationType.UPDATE)
    }

    /** Deletes the folder and everything under it — a folder is never left half-emptied. */
    suspend fun delete(workspaceId: String, id: String) {
        val target = folders.findById(id) ?: return
        val subtree = folders.findByPathPrefix(workspaceId, target.path)
        val timestamp = now()

        database.withTransaction {
            (subtree + target).distinctBy { it.id }.forEach { folder ->
                folders.softDelete(folder.id, timestamp)
                val opId = operations.enqueue(
                    SyncOperationEntity(
                        recordId = folder.id,
                        entityType = EntityType.FOLDER,
                        operation = SyncOperationType.DELETE,
                        clientMutationId = newId(),
                        createdAt = timestamp,
                    ),
                )
                operations.removeSuperseded(folder.id, opId)
            }
        }
        syncTrigger.syncNow()
    }

    private fun withBumpedSync(entity: FolderEntity): FolderEntity = entity.copy(
        sync = entity.sync.copy(
            updatedAt = now(),
            syncStatus = SyncStatus.PENDING_UPDATE,
            clientMutationId = newId(),
        ),
    )

    private suspend fun enqueue(entity: FolderEntity, operation: SyncOperationType) {
        database.withTransaction {
            folders.upsert(entity)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = entity.id,
                    entityType = EntityType.FOLDER,
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
