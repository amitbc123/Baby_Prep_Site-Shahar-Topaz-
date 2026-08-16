package com.oryareach.core.sync

import com.oryareach.core.common.AppError
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.SyncOperationType

/**
 * A record as the server holds it: an opaque envelope plus the metadata the server is
 * allowed to see. The server can order and address these; it cannot read them.
 */
data class RemoteRecord(
    val id: String,
    val entityType: EntityType,
    val ciphertext: ByteArray,
    val version: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
) {
    // ByteArray uses identity equality, which would break every comparison in tests and in
    // conflict detection, so both halves are overridden.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is RemoteRecord &&
                id == other.id &&
                entityType == other.entityType &&
                ciphertext.contentEquals(other.ciphertext) &&
                version == other.version &&
                updatedAt == other.updatedAt &&
                deletedAt == other.deletedAt
            )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + version
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (deletedAt?.hashCode() ?: 0)
        return result
    }
}

/** One queued local change, already encrypted, ready to send. */
data class PushRequest(
    val recordId: String,
    val entityType: EntityType,
    val operation: SyncOperationType,
    val ciphertext: ByteArray,
    /** The version this edit was made against. The server rejects a stale one. */
    val baseVersion: Int,
    val clientMutationId: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is PushRequest &&
                recordId == other.recordId &&
                entityType == other.entityType &&
                operation == other.operation &&
                ciphertext.contentEquals(other.ciphertext) &&
                baseVersion == other.baseVersion &&
                clientMutationId == other.clientMutationId
            )

    override fun hashCode(): Int {
        var result = recordId.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + baseVersion
        result = 31 * result + clientMutationId.hashCode()
        return result
    }
}

sealed interface PushResult {
    val recordId: String

    data class Applied(override val recordId: String, val newVersion: Int) : PushResult

    /**
     * The server holds a newer version. Its record comes back so the UI can show both sides;
     * the engine never picks a winner on its own.
     */
    data class Conflict(override val recordId: String, val server: RemoteRecord) : PushResult

    /** Transient: the operation stays queued and is retried. */
    data class Failed(override val recordId: String, val error: AppError) : PushResult
}

interface RecordRemoteDataSource {
    suspend fun push(requests: List<PushRequest>): List<PushResult>

    /** Records changed strictly after [since]; null pulls everything. */
    suspend fun pull(since: Long?, limit: Int): List<RemoteRecord>
}

/**
 * The local side of sync. Implemented over Room in `:core:database`; kept as an interface so
 * the engine's logic can be tested without Android.
 */
interface SyncStore {
    suspend fun pendingChanges(limit: Int): List<PushRequest>
    suspend fun markSynced(recordId: String, version: Int)
    suspend fun markConflict(recordId: String, server: RemoteRecord)
    suspend fun recordFailure(recordId: String, error: AppError)
    suspend fun applyRemote(records: List<RemoteRecord>)
    suspend fun pullCursor(): Long?
    suspend fun savePullCursor(cursor: Long)
}

data class SyncOutcome(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val failures: Int = 0,
) {
    val hasConflicts: Boolean get() = conflicts > 0

    /** WorkManager should retry when something transient failed, but not for conflicts. */
    val shouldRetry: Boolean get() = failures > 0
}
