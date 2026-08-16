package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.MenstrualCycleEntity
import com.oryareach.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MenstrualCycleDao {

    /** Newest first: history screens and prediction both want the most recent cycles. */
    @Query(
        """
        SELECT * FROM menstrual_cycles
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL
        ORDER BY start_date DESC
        """,
    )
    fun observeAll(workspaceId: String): Flow<List<MenstrualCycleEntity>>

    @Query(
        """
        SELECT * FROM menstrual_cycles
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL AND end_date IS NULL
        ORDER BY start_date DESC
        LIMIT 1
        """,
    )
    fun observeOngoing(workspaceId: String): Flow<MenstrualCycleEntity?>

    @Query("SELECT * FROM menstrual_cycles WHERE id = :id")
    suspend fun findById(id: String): MenstrualCycleEntity?

    @Upsert
    suspend fun upsert(cycle: MenstrualCycleEntity)

    @Upsert
    suspend fun upsertAll(cycles: List<MenstrualCycleEntity>)

    @Query("SELECT * FROM menstrual_cycles WHERE sync_status != :synced")
    suspend fun pendingSync(synced: SyncStatus = SyncStatus.SYNCED): List<MenstrualCycleEntity>

    @Query("UPDATE menstrual_cycles SET sync_status = :status, version = :version WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus, version: Int)

    @Query(
        """
        UPDATE menstrual_cycles
        SET deleted_at = :deletedAt, sync_status = :status, updated_at = :deletedAt
        WHERE id = :id
        """,
    )
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
    )

    @Query("DELETE FROM menstrual_cycles WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT MAX(updated_at) FROM menstrual_cycles WHERE workspace_id = :workspaceId")
    suspend fun latestUpdatedAt(workspaceId: String): Long?
}
