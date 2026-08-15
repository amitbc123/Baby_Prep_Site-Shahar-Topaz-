package com.oryareach.core.model

import kotlin.time.Instant

/**
 * Sync bookkeeping carried by every record that can reach the server.
 *
 * Deliberately separate from the domain types: `Task` describes a task, `SyncMeta` describes
 * that task's relationship with the server, and the two change for different reasons.
 */
data class SyncMeta(
    val workspaceId: String,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Tombstone. Set instead of hard-deleting, so the delete can propagate. */
    val deletedAt: Instant? = null,
    /** Optimistic concurrency: the server rejects a write whose version is stale. */
    val version: Int = 1,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    /** Makes a retried upload idempotent when the first attempt's response was lost. */
    val clientMutationId: String? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null
}
