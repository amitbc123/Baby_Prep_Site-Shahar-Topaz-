package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.ImportantDateEntity
import com.oryareach.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportantDateDao {

    @Query(
        """
        SELECT * FROM important_dates
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL
        ORDER BY date ASC
        """,
    )
    fun observeAll(workspaceId: String): Flow<List<ImportantDateEntity>>

    @Query("SELECT * FROM important_dates WHERE id = :id")
    suspend fun findById(id: String): ImportantDateEntity?

    @Upsert
    suspend fun upsert(date: ImportantDateEntity)

    @Query("SELECT * FROM important_dates WHERE sync_status != :synced")
    suspend fun pendingSync(synced: SyncStatus = SyncStatus.SYNCED): List<ImportantDateEntity>

    @Query("UPDATE important_dates SET sync_status = :status, version = :version WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus, version: Int)

    @Query(
        """
        UPDATE important_dates
        SET deleted_at = :deletedAt, sync_status = :status, updated_at = :deletedAt
        WHERE id = :id
        """,
    )
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
    )

    @Query("DELETE FROM important_dates WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT MAX(updated_at) FROM important_dates WHERE workspace_id = :workspaceId")
    suspend fun latestUpdatedAt(workspaceId: String): Long?
}
