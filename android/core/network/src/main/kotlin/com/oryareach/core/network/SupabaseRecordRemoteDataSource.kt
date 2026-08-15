package com.oryareach.core.network

import android.util.Base64
import com.oryareach.core.common.AppError
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.sync.PushRequest
import com.oryareach.core.sync.PushResult
import com.oryareach.core.sync.RecordRemoteDataSource
import com.oryareach.core.sync.RemoteRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Talks to the two sync RPCs. Everything crossing this boundary is already encrypted — this
 * class handles transport encoding only and never sees a plaintext value.
 */
class SupabaseRecordRemoteDataSource(
    private val client: SupabaseClient,
    private val workspaceId: () -> String?,
) : RecordRemoteDataSource {

    override suspend fun push(requests: List<PushRequest>): List<PushResult> {
        if (requests.isEmpty()) return emptyList()
        val workspace = workspaceId()
            ?: return requests.map { PushResult.Failed(it.recordId, AppError.Network.Unauthorized) }

        val payload = PushParams(
            workspace = workspace,
            records = requests.map { request ->
                PushRecordDto(
                    id = request.recordId,
                    entityType = request.entityType.wireName,
                    ciphertext = request.ciphertext.toBase64(),
                    baseVersion = request.baseVersion,
                    clientMutationId = request.clientMutationId,
                    deleted = request.operation == SyncOperationType.DELETE,
                )
            },
        )

        return try {
            client.postgrest.rpc("push_records", payload.asJsonObject())
                .decodeList<PushResultDto>()
                .map(::toPushResult)
        } catch (e: Exception) {
            // Transport failure applies to the whole batch: nothing is known to have landed,
            // and every operation stays queued for the next run.
            val error = e.toAppError()
            requests.map { PushResult.Failed(it.recordId, error) }
        }
    }

    override suspend fun pull(since: Long?, limit: Int): List<RemoteRecord> {
        val workspace = workspaceId() ?: return emptyList()

        return client.postgrest
            .rpc("pull_records", PullParams(workspace, since, limit).asJsonObject())
            .decodeList<PullRecordDto>()
            .mapNotNull { dto ->
                // An unknown entity type means this device is older than the writer. Skip it
                // rather than crash; a later app version will pull it again by cursor.
                val type = EntityType.fromWireName(dto.entityType) ?: return@mapNotNull null
                RemoteRecord(
                    id = dto.id,
                    entityType = type,
                    ciphertext = dto.ciphertext.fromBase64(),
                    version = dto.version,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                )
            }
    }

    private fun toPushResult(dto: PushResultDto): PushResult = when (dto.status) {
        "applied" -> PushResult.Applied(dto.id, dto.version)

        "conflict" -> {
            val type = dto.entityType?.let(EntityType::fromWireName)
            if (type == null || dto.ciphertext == null) {
                PushResult.Failed(dto.id, AppError.Unexpected("malformed conflict for ${dto.id}"))
            } else {
                PushResult.Conflict(
                    recordId = dto.id,
                    server = RemoteRecord(
                        id = dto.id,
                        entityType = type,
                        ciphertext = dto.ciphertext.fromBase64(),
                        version = dto.version,
                        updatedAt = dto.updatedAt?.toLong() ?: 0L,
                        deletedAt = dto.deletedAt?.toLong(),
                    ),
                )
            }
        }

        else -> PushResult.Failed(dto.id, AppError.Unexpected("unknown push status ${dto.status}"))
    }
}

private inline fun <reified T> T.asJsonObject(): JsonObject =
    Json.encodeToJsonElement(this).jsonObject

private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

private fun Exception.toAppError(): AppError = when {
    this is java.net.UnknownHostException -> AppError.Network.Offline
    this is java.net.SocketTimeoutException -> AppError.Network.Timeout
    message?.contains("401") == true || message?.contains("JWT") == true ->
        AppError.Network.Unauthorized
    else -> AppError.Network.Server(status = 0)
}
