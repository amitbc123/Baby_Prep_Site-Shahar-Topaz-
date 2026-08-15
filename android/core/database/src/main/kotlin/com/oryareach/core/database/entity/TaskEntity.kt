package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.RecurrenceFrequency
import com.oryareach.core.model.TaskCategory

@Entity(
    tableName = "tasks",
    indices = [
        // The sync engine's push query: everything not yet settled with the server.
        Index(value = ["sync_status"]),
        // The pull cursor and the list screen both order by this.
        Index(value = ["workspace_id", "updated_at"]),
        Index(value = ["workspace_id", "category"]),
        Index(value = ["due_date"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: Priority,
    val done: Boolean,
    /** ISO-8601 `yyyy-MM-dd`, so ordering as text matches ordering as a date. */
    @ColumnInfo(name = "due_date") val dueDate: String?,
    val assignee: Assignee?,
    val note: String?,
    @ColumnInfo(name = "recurrence_frequency") val recurrenceFrequency: RecurrenceFrequency?,
    @ColumnInfo(name = "recurrence_interval") val recurrenceInterval: Int?,
    val tags: List<String>,
    @Embedded val sync: SyncMetaEntity,
)
