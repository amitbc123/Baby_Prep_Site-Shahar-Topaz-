package com.oryareach.core.database.mapper

import com.oryareach.core.database.entity.MenstrualCycleEntity
import com.oryareach.core.database.entity.SyncMetaEntity
import com.oryareach.core.database.entity.TaskEntity
import com.oryareach.core.model.MenstrualCycle
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.model.Task
import com.oryareach.core.sync.RemoteRecord
import kotlinx.datetime.LocalDate

/**
 * Entity to domain and back.
 *
 * Dates are stored as ISO-8601 text so that ordering as text matches ordering as a date,
 * which lets SQLite sort and range-filter them without a conversion.
 */

fun TaskEntity.toTask() = Task(
    id = id,
    title = title,
    category = category,
    priority = priority,
    done = done,
    dueDate = dueDate?.let(LocalDate::parse),
    assignee = assignee,
    note = note,
)

fun MenstrualCycleEntity.toCycle() = MenstrualCycle(
    id = id,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    note = note,
)

/** Builds the local row for a record that arrived from the server, already synced. */
fun Task.toEntity(workspaceId: String, record: RemoteRecord, now: Long) = TaskEntity(
    id = id,
    title = title,
    category = category,
    priority = priority,
    done = done,
    dueDate = dueDate?.toString(),
    assignee = assignee,
    note = note,
    sync = record.toSyncMeta(workspaceId, now),
)

fun MenstrualCycle.toEntity(workspaceId: String, record: RemoteRecord, now: Long) =
    MenstrualCycleEntity(
        id = id,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        note = note,
        sync = record.toSyncMeta(workspaceId, now),
    )

private fun RemoteRecord.toSyncMeta(workspaceId: String, now: Long) = SyncMetaEntity(
    workspaceId = workspaceId,
    // The server owns attribution; a pulled row carries no local author.
    createdBy = "",
    createdAt = now,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    version = version,
    syncStatus = SyncStatus.SYNCED,
    clientMutationId = null,
)
