package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "important_dates",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["workspace_id", "updated_at"]),
        Index(value = ["date"]),
    ],
)
data class ImportantDateEntity(
    @PrimaryKey val id: String,
    /** ISO-8601 `yyyy-MM-dd`, so ordering as text matches ordering as a date. */
    val date: String,
    val title: String,
    val wish: String?,
    @Embedded val sync: SyncMetaEntity,
)
