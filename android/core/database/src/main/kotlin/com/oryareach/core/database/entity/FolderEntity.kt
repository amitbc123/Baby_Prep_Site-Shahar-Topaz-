package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["workspace_id", "updated_at"]),
        Index(value = ["workspace_id", "parent_id"]),
        Index(value = ["workspace_id", "path"]),
    ],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    /** Materialized path, e.g. `/<ancestor>/.../<id>/` — see [com.oryareach.core.model.Folder]. */
    val path: String,
    @Embedded val sync: SyncMetaEntity,
)
