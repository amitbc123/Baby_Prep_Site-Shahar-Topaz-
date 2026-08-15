package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import com.oryareach.core.model.SyncStatus

/**
 * Embedded into every syncable entity so the sync engine can treat them uniformly without
 * each table inventing its own bookkeeping columns.
 */
data class SyncMetaEntity(
    @ColumnInfo(name = "workspace_id") val workspaceId: String,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "version") val version: Int = 1,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    @ColumnInfo(name = "client_mutation_id") val clientMutationId: String? = null,
)
