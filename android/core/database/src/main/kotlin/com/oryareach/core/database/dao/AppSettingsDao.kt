package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.AppSettingsEntity
import com.oryareach.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/** One row per workspace — `LIMIT 1` is the whole query shape, not a real list. */
@Dao
interface AppSettingsDao {

    @Query(
        """
        SELECT * FROM app_settings
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    fun observe(workspaceId: String): Flow<AppSettingsEntity?>

    @Query(
        """
        SELECT * FROM app_settings
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun find(workspaceId: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = :id")
    suspend fun findById(id: String): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE sync_status != :synced")
    suspend fun pendingSync(synced: SyncStatus = SyncStatus.SYNCED): List<AppSettingsEntity>

    @Query("UPDATE app_settings SET sync_status = :status, version = :version WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus, version: Int)

    @Query("SELECT MAX(updated_at) FROM app_settings WHERE workspace_id = :workspaceId")
    suspend fun latestUpdatedAt(workspaceId: String): Long?
}
