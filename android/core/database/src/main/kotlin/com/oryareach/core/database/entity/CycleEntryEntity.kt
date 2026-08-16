package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.oryareach.core.model.FlowLevel
import com.oryareach.core.model.Mood
import com.oryareach.core.model.PainLevel
import com.oryareach.core.model.Symptom

@Entity(
    tableName = "cycle_entries",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["workspace_id", "updated_at"]),
        // The calendar reads a date range, and logging a day looks up its existing entry by
        // date first (not a DB-level unique constraint — the repository enforces "one entry
        // per date" by finding and updating the existing row, which plays nicer with sync
        // than a constraint that would reject a legitimate update).
        Index(value = ["workspace_id", "date"]),
    ],
)
data class CycleEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val flow: FlowLevel?,
    val symptoms: List<Symptom>,
    val mood: List<Mood>,
    val pain: PainLevel?,
    val note: String?,
    @Embedded val sync: SyncMetaEntity,
)
