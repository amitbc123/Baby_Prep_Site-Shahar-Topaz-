package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How far the last successful pull got, per workspace.
 *
 * Kept in the same database as the records so the cursor and the rows it describes advance
 * in one transaction. Storing it separately (in preferences, say) would let the cursor move
 * past rows that were never actually written, silently skipping them forever.
 */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey @ColumnInfo(name = "workspace_id") val workspaceId: String,
    @ColumnInfo(name = "pulled_through") val pulledThrough: Long,
)
