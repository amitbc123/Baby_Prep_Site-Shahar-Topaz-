package com.oryareach.core.database.repository

import androidx.room.withTransaction
import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.entity.TaskEntity
import com.oryareach.core.database.mapper.toTask
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.Priority
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.model.Task
import com.oryareach.core.model.TaskCategory
import com.oryareach.core.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * The write path for tasks: every mutation lands in Room and the outbox in one transaction,
 * then kicks the sync worker. The UI never talks to `TaskDao` directly.
 */
class TaskRepository(
    private val database: OrYareachDatabase,
    private val syncTrigger: SyncTrigger,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val tasks get() = database.taskDao()
    private val operations get() = database.syncOperationDao()

    fun observeAll(workspaceId: String): Flow<List<Task>> =
        tasks.observeAll(workspaceId).map { list -> list.map { it.toTask() } }

    suspend fun create(
        workspaceId: String,
        userId: String,
        title: String,
        category: TaskCategory,
        priority: Priority = Priority.NORMAL,
        assignee: Assignee? = null,
        note: String? = null,
        done: Boolean = false,
    ) {
        val timestamp = now()
        val entity = TaskEntity(
            id = newId(),
            title = title,
            category = category,
            priority = priority,
            done = done,
            dueDate = null,
            assignee = assignee,
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

    suspend fun update(
        id: String,
        title: String,
        category: TaskCategory,
        priority: Priority,
        assignee: Assignee?,
        note: String?,
    ) {
        val existing = tasks.findById(id) ?: return
        val updated = existing.copy(
            title = title,
            category = category,
            priority = priority,
            assignee = assignee,
            note = note,
        )
        enqueue(withBumpedSync(updated), SyncOperationType.UPDATE)
    }

    suspend fun toggleDone(id: String) {
        val existing = tasks.findById(id) ?: return
        enqueue(withBumpedSync(existing.copy(done = !existing.done)), SyncOperationType.UPDATE)
    }

    suspend fun delete(id: String) {
        val timestamp = now()
        database.withTransaction {
            tasks.softDelete(id, timestamp)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = id,
                    entityType = EntityType.TASK,
                    operation = SyncOperationType.DELETE,
                    clientMutationId = newId(),
                    createdAt = timestamp,
                ),
            )
            operations.removeSuperseded(id, opId)
        }
        syncTrigger.syncNow()
    }

    private fun withBumpedSync(entity: TaskEntity): TaskEntity = entity.copy(
        sync = entity.sync.copy(
            updatedAt = now(),
            syncStatus = SyncStatus.PENDING_UPDATE,
            clientMutationId = newId(),
        ),
    )

    private suspend fun enqueue(entity: TaskEntity, operation: SyncOperationType) {
        database.withTransaction {
            tasks.upsert(entity)
            val opId = operations.enqueue(
                SyncOperationEntity(
                    recordId = entity.id,
                    entityType = EntityType.TASK,
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
