package com.oryareach.core.network

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import com.oryareach.core.sync.DocumentBlobStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage

private const val BUCKET = "documents"

/**
 * The bucket is private — RLS on `storage.objects` is what actually restricts access, keyed
 * off the workspace id in each object's path. `downloadAuthenticated` (not `download`, which
 * assumes a public bucket) is what carries the caller's JWT through that check.
 */
class SupabaseDocumentBlobStore(private val client: SupabaseClient) : DocumentBlobStore {

    override suspend fun upload(path: String, bytes: ByteArray): AppResult<Unit> = try {
        client.storage.from(BUCKET).upload(path, bytes)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(e.toBlobError())
    }

    override suspend fun download(path: String): AppResult<ByteArray> = try {
        AppResult.Success(client.storage.from(BUCKET).downloadAuthenticated(path))
    } catch (e: Exception) {
        AppResult.Failure(e.toBlobError())
    }
}

private fun Exception.toBlobError(): AppError = when {
    this is java.net.UnknownHostException -> AppError.Network.Offline
    this is java.net.SocketTimeoutException -> AppError.Network.Timeout
    else -> AppError.Unexpected(message ?: this::class.simpleName.orEmpty())
}
