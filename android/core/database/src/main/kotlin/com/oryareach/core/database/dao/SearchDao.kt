package com.oryareach.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oryareach.core.database.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchIndexEntity)

    @Query("DELETE FROM search_index WHERE recordId = :recordId")
    suspend fun deleteByRecord(recordId: String)

    /** `MATCH` needs the raw FTS query syntax — [com.oryareach.core.database.SearchIndexer]
     * is responsible for turning a user's free-text query into a safe `MATCH` expression
     * (each term suffixed with `*` for prefix matching) before it reaches here. */
    @Query(
        """
        SELECT * FROM search_index
        WHERE search_index MATCH :ftsQuery AND workspaceId = :workspaceId
        LIMIT 50
        """,
    )
    fun search(ftsQuery: String, workspaceId: String): Flow<List<SearchIndexEntity>>
}
