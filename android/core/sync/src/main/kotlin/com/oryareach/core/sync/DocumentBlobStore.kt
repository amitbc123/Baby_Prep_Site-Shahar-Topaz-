package com.oryareach.core.sync

import com.oryareach.core.common.AppResult

/**
 * The file-bytes half of document storage — separate from [RecordRemoteDataSource], which
 * only ever carries small encrypted JSON payloads. Implemented over Supabase Storage in
 * `:core:network`; kept as an interface so `:core:database`'s `DocumentRepository` does not
 * depend on the network module, matching the seam [SyncTrigger] already draws.
 */
interface DocumentBlobStore {
    /** [path] is `{workspaceId}/{documentId}`. [bytes] are already encrypted. */
    suspend fun upload(path: String, bytes: ByteArray): AppResult<Unit>

    suspend fun download(path: String): AppResult<ByteArray>
}
