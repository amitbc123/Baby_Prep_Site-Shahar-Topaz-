package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recorded period. Only user-entered facts: predictions are derived at read time from the
 * recorded history, never stored, so they cannot go stale.
 */
@Entity(
    tableName = "menstrual_cycles",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["workspace_id", "updated_at"]),
        // Prediction reads the most recent cycles in start-date order.
        Index(value = ["workspace_id", "start_date"]),
    ],
)
data class MenstrualCycleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?,
    val note: String?,
    @Embedded val sync: SyncMetaEntity,
)
