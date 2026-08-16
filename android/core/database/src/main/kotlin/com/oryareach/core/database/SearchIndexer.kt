package com.oryareach.core.database

import com.oryareach.core.database.entity.SearchIndexEntity
import com.oryareach.core.model.EntityType

/**
 * Keeps `search_index` (see [com.oryareach.core.database.entity.SearchIndexEntity]) in sync
 * with every other table. Every repository calls [index] from inside the same transaction as
 * its normal upsert, and [remove] from inside the same transaction as its soft-delete —
 * there is deliberately no separate "reindex everything" batch job, so the index can never
 * drift out of sync with what's actually on screen.
 */
class SearchIndexer(private val database: OrYareachDatabase) {

    private val search get() = database.searchDao()

    /** Delete-then-insert, not a conflict-replacing upsert: `recordId` has no uniqueness
     * constraint Room/SQLite can enforce on an FTS table (only the implicit `rowid` does), so
     * an `OnConflictStrategy.REPLACE` insert would never actually fire on a re-index — it
     * would just accumulate a duplicate row per edit. Callers already run this inside their
     * own `withTransaction`, so the two statements stay atomic with the rest of the write. */
    suspend fun index(entityType: EntityType, recordId: String, workspaceId: String, title: String, body: String) {
        search.deleteByRecord(recordId)
        search.insert(
            SearchIndexEntity(
                entityType = entityType.wireName,
                recordId = recordId,
                workspaceId = workspaceId,
                title = title,
                body = body,
            ),
        )
    }

    suspend fun remove(recordId: String) = search.deleteByRecord(recordId)
}
