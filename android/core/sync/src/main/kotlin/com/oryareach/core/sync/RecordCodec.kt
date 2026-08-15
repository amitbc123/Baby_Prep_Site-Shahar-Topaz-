package com.oryareach.core.sync

import com.oryareach.core.common.AppResult
import com.oryareach.core.common.map
import com.oryareach.core.crypto.RecordCipher
import com.oryareach.core.crypto.WorkspaceKey
import com.oryareach.core.model.EntityType

/**
 * Supplies the workspace key. An interface so the key can live behind the Keystore and the
 * biometric lock without this module knowing about either.
 */
fun interface WorkspaceKeyProvider {
    /** Null when the workspace is locked or this device has not been paired yet. */
    fun current(): WorkspaceKey?
}

/**
 * Turns a record's serialized payload into the opaque blob the server stores, and back.
 *
 * This is the boundary where plaintext stops. Everything above it works with real values;
 * everything below it — the data source, the network, the database on the server — sees only
 * ciphertext.
 */
class RecordCodec(
    private val keys: WorkspaceKeyProvider,
    private val cipher: RecordCipher = RecordCipher(),
) {

    fun encode(entityType: EntityType, recordId: String, json: String): AppResult<ByteArray> {
        val key = keys.current()
            ?: return AppResult.Failure(com.oryareach.core.common.AppError.Crypto.KeyUnavailable)

        return AppResult.Success(
            cipher.encrypt(key, json.toByteArray(Charsets.UTF_8), associatedData(entityType, recordId)),
        )
    }

    fun decode(entityType: EntityType, recordId: String, ciphertext: ByteArray): AppResult<String> {
        val key = keys.current()
            ?: return AppResult.Failure(com.oryareach.core.common.AppError.Crypto.KeyUnavailable)

        return cipher.decrypt(key, ciphertext, associatedData(entityType, recordId))
            .map { it.toString(Charsets.UTF_8) }
    }

    /**
     * Binds each ciphertext to its identity, so a blob cannot be moved to a different record
     * or reinterpreted as a different entity type and still authenticate.
     *
     * Version is deliberately excluded: the server assigns a new version on accept, and
     * including it would make every accepted record undecryptable afterwards.
     */
    private fun associatedData(entityType: EntityType, recordId: String): ByteArray =
        "${entityType.wireName}:$recordId".toByteArray(Charsets.UTF_8)
}
