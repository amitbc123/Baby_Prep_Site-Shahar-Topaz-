package com.oryareach.core.crypto

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import org.bouncycastle.crypto.hpke.HPKE

/**
 * Transfers the workspace key to a joining device using HPKE base mode
 * (RFC 9180: X25519 + HKDF-SHA256 + ChaCha20-Poly1305).
 *
 * The inviting device seals the workspace key to the joining device's public key. The server
 * relays the result but cannot open it — it never holds the recipient's private key.
 *
 * Blob layout: `[version:1][encapsulated sender key:32][ciphertext || tag]`
 */
class KeyWrap {

    private val hpke = HPKE(
        HPKE.mode_base,
        HPKE.kem_X25519_SHA256,
        HPKE.kdf_HKDF_SHA256,
        HPKE.aead_CHACHA20_POLY1305,
    )

    fun generateDeviceKeyPair(): DeviceKeyPair {
        val pair = hpke.generatePrivateKey()
        return DeviceKeyPair(
            publicKey = hpke.serializePublicKey(pair.public),
            privateKey = hpke.serializePrivateKey(pair.private),
        )
    }

    /** Size of the encapsulated sender key for this suite (32 for X25519). */
    private val encSize: Int = hpke.encSize

    fun wrap(workspaceKey: WorkspaceKey, recipientPublicKey: ByteArray): ByteArray {
        require(recipientPublicKey.size == DeviceKeyPair.KEY_BYTES) {
            "recipient public key must be ${DeviceKeyPair.KEY_BYTES} bytes"
        }
        val recipient = hpke.deserializePublicKey(recipientPublicKey)
        val secret = workspaceKey.bytes()

        val sealed = try {
            hpke.seal(recipient, HPKE_INFO, ByteArray(0), secret, null, null, null)
        } finally {
            secret.fill(0)
        }

        // Bouncy Castle returns [ciphertext, encapsulation] in that order.
        val ciphertext = sealed[0]
        val enc = sealed[1]
        check(enc.size == encSize) { "unexpected HPKE encapsulated key size ${enc.size}" }

        return ByteArray(1 + enc.size + ciphertext.size).also { blob ->
            blob[0] = BLOB_VERSION
            enc.copyInto(blob, destinationOffset = 1)
            ciphertext.copyInto(blob, destinationOffset = 1 + enc.size)
        }
    }

    fun unwrap(blob: ByteArray, recipient: DeviceKeyPair): AppResult<WorkspaceKey> {
        if (blob.size <= 1 + encSize) {
            return AppResult.Failure(AppError.Crypto.DecryptionFailed)
        }
        if (blob[0] != BLOB_VERSION) {
            return AppResult.Failure(AppError.Crypto.UnsupportedEnvelopeVersion)
        }

        val enc = blob.copyOfRange(1, 1 + encSize)
        val ciphertext = blob.copyOfRange(1 + encSize, blob.size)

        return try {
            val keyPair = hpke.deserializePrivateKey(recipient.privateKeyBytes(), recipient.publicKey)
            val secret = hpke.open(enc, keyPair, HPKE_INFO, ByteArray(0), ciphertext, null, null, null)
            try {
                AppResult.Success(WorkspaceKey(secret))
            } finally {
                secret.fill(0)
            }
        } catch (_: Exception) {
            // Any failure here — bad tag, wrong recipient, malformed encapsulation — is the
            // same thing to the caller: this blob is not openable by this device.
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
        }
    }

    private companion object {
        const val BLOB_VERSION: Byte = 1
        val HPKE_INFO = "oryareach:workspace-key:v1".toByteArray(Charsets.UTF_8)
    }
}
