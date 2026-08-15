package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.CycleEntryEntity
import com.oryareach.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEntryDao {

    @Query(
        """
        SELECT * FROM cycle_entries
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL AND date BETWEEN :start AND :end
        ORDER BY date ASC
        """,
    )
    fun observeInRange(workspaceId: String, start: String, end: String): Flow<List<CycleEntryEntity>>

    @Query(
        """
        SELECT * FROM cycle_entries
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL AND date = :date
        LIMIT 1
        """,
    )
    suspend fun findByDate(workspaceId: String, date: String): CycleEntryEntity?

    @Query("SELECT * FROM cycle_entries WHERE id = :id")
    suspend fun findById(id: String): CycleEntryEntity?

    @Upsert
    suspend fun upsert(entry: CycleEntryEntity)

    @Query("SELECT * FROM cycle_entries WHERE sync_status != :synced")
    suspend fun pendingSync(synced: SyncStatus = SyncStatus.SYNCED): List<CycleEntryEntity>

    @Query("UPDATE cycle_entries SET sync_status = :status, version = :version WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus, version: Int)

    @Query(
        """
        UPDATE cycle_entries
        SET deleted_at = :deletedAt, sync_status = :status, updated_at = :deletedAt
        WHERE id = :id
        """,
    )
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
    )

    @Query("DELETE FROM cycle_entries WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT MAX(updated_at) FROM cycle_entries WHERE workspace_id = :workspaceId")
    suspend fun latestUpdatedAt(workspaceId: String): Long?
}
