package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oryareach.core.database.entity.FolderEntity
import com.oryareach.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    /** `parentId = null` (top-level "root") needs its own query: SQL `= NULL` never matches. */
    @Query(
        """
        SELECT * FROM folders
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL AND parent_id IS :parentId
        ORDER BY name ASC
        """,
    )
    fun observeChildren(workspaceId: String, parentId: String?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun findById(id: String): FolderEntity?

    /** Everything under [pathPrefix], for a cascading delete of a folder's subtree. */
    @Query(
        """
        SELECT * FROM folders
        WHERE workspace_id = :workspaceId AND deleted_at IS NULL AND path LIKE :pathPrefix || '%'
        """,
    )
    suspend fun findByPathPrefix(workspaceId: String, pathPrefix: String): List<FolderEntity>

    @Upsert
    suspend fun upsert(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE sync_status != :synced")
    suspend fun pendingSync(synced: SyncStatus = SyncStatus.SYNCED): List<FolderEntity>

    @Query("UPDATE folders SET sync_status = :status, version = :version WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus, version: Int)

    @Query(
        """
        UPDATE folders
        SET deleted_at = :deletedAt, sync_status = :status, updated_at = :deletedAt
        WHERE id = :id
        """,
    )
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
    )

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT MAX(updated_at) FROM folders WHERE workspace_id = :workspaceId")
    suspend fun latestUpdatedAt(workspaceId: String): Long?
}
