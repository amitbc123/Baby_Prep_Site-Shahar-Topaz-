package com.oryareach.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the push_records / pull_records RPCs.
 *
 * Ciphertext travels base64-encoded because JSON has no byte type; the server decodes it to
 * bytea. Field names match the SQL exactly, so a rename on either side breaks loudly here
 * rather than silently dropping a value.
 */
@Serializable
internal data class PushRecordDto(
    val id: String,
    @SerialName("entity_type") val entityType: String,
    val ciphertext: String,
    @SerialName("base_version") val baseVersion: Int,
    @SerialName("client_mutation_id") val clientMutationId: String,
    val deleted: Boolean,
)

@Serializable
internal data class PushResultDto(
    val id: String,
    val status: String,
    val version: Int,
    @SerialName("entity_type") val entityType: String? = null,
    val ciphertext: String? = null,
    @SerialName("updated_at") val updatedAt: Double? = null,
    @SerialName("deleted_at") val deletedAt: Double? = null,
)

@Serializable
internal data class PullRecordDto(
    val id: String,
    @SerialName("entity_type") val entityType: String,
    val ciphertext: String,
    val version: Int,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
internal data class PushParams(
    @SerialName("p_workspace") val workspace: String,
    @SerialName("p_records") val records: List<PushRecordDto>,
)

@Serializable
internal data class PullParams(
    @SerialName("p_workspace") val workspace: String,
    @SerialName("p_since") val since: Long?,
    @SerialName("p_limit") val limit: Int,
)
