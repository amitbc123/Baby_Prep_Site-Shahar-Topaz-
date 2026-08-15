package com.oryareach.core.database.repository

import com.oryareach.core.database.OrYareachDatabase
import com.oryareach.core.model.EntityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

data class SearchResult(
    val entityType: EntityType,
    val recordId: String,
    val title: String,
    val snippet: String,
)

/**
 * Read path for the FTS4 index every other repository writes to (see `SearchIndexer`). Query
 * sanitization lives here rather than in the DAO: `MATCH` uses FTS's own query syntax, and a
 * user's raw free-text input (which can contain `"`, `*`, `-`, `AND`/`OR`/`NOT`) must never
 * reach it unescaped — a bare `MATCH :userInput` would let a search for `a b` accidentally
 * behave as `a AND b` at best and throw a syntax error at worst.
 */
class SearchRepository(private val database: OrYareachDatabase) {

    private val dao get() = database.searchDao()

    fun search(query: String, workspaceId: String): Flow<List<SearchResult>> {
        val ftsQuery = toFtsQuery(query) ?: return emptyFlow()
        return dao.search(ftsQuery, workspaceId).map { rows ->
            rows.map {
                SearchResult(
                    entityType = EntityType.fromWireName(it.entityType) ?: EntityType.TASK,
                    recordId = it.recordId,
                    title = it.title,
                    snippet = it.body,
                )
            }
        }
    }

    /** Splits on whitespace, drops FTS special characters from each term (rather than
     * quoting them, which would turn the term into a literal phrase match instead of a
     * prefix match), and suffixes every term with `*` for prefix matching — searching
     * "milk" should find "almond milk powder". Returns null for a query with nothing left
     * to search on, so an empty/punctuation-only query shows no results instead of matching
     * everything. */
    private fun toFtsQuery(raw: String): String? {
        val terms = raw
            .split(Regex("\\s+"))
            .map { it.filter(Char::isLetterOrDigit) }
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "$it*" }
    }
}
