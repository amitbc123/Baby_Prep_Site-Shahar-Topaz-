package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.SyncConflictEntity
import com.oryareach.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    @Query("SELECT pulled_through FROM sync_cursors WHERE workspace_id = :workspaceId")
    suspend fun cursor(workspaceId: String): Long?

    @Upsert
    suspend fun saveCursor(cursor: SyncCursorEntity)

    @Upsert
    suspend fun saveConflict(conflict: SyncConflictEntity)

    @Query("SELECT * FROM sync_conflicts ORDER BY detected_at ASC")
    fun observeConflicts(): Flow<List<SyncConflictEntity>>

    @Query("SELECT COUNT(*) FROM sync_conflicts")
    fun observeConflictCount(): Flow<Int>

    @Query("SELECT * FROM sync_conflicts WHERE record_id = :recordId")
    suspend fun conflict(recordId: String): SyncConflictEntity?

    @Query("DELETE FROM sync_conflicts WHERE record_id = :recordId")
    suspend fun clearConflict(recordId: String)
}
