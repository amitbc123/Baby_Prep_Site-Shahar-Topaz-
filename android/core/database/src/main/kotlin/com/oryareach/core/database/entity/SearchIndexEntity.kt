package com.oryareach.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * A hand-maintained search index, not a Room `contentEntity` mirror of one table — search
 * spans seven different entity types, and Room's FTS `contentEntity` only mirrors one table
 * at a time. Every repository upserts/removes its own rows here alongside its normal write
 * (see [com.oryareach.core.database.SearchIndexer]), so this table's content is always a
 * projection of whatever is currently in the regular tables, never a separate source of truth.
 *
 * `@Fts4`, not `@Fts5`: `docs/architecture/007-encryption.md` names FTS5, but Room 2.8.4 (the
 * pinned version here) has no `@Fts5` annotation at all — only `@Fts3`/`@Fts4` exist in
 * `androidx.room` (checked directly against the jar; this is a hard library limitation, not a
 * risk-aversion call). Room's declarative FTS support has never covered FTS5, only the
 * underlying SQLite engine has offered it; using it would mean hand-writing the virtual table
 * and losing Room's generated DAO binding. FTS4 is the real, current state — the ADR should be
 * corrected to match rather than left describing something Room cannot do.
 */
@Entity(tableName = "search_index")
@Fts4
data class SearchIndexEntity(
    val entityType: String,
    val recordId: String,
    val workspaceId: String,
    val title: String,
    val body: String,
)
