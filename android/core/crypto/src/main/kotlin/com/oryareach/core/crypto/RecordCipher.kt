package com.oryareach.core.crypto

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

/**
 * Authenticated encryption for a single record payload.
 *
 * Each write derives a fresh key with HKDF over a random salt, which is what makes the
 * all-zero nonce safe: the (key, nonce) pair can only repeat if the same 16-byte salt is
 * drawn twice. Deriving per write rather than reusing the workspace key with a random
 * nonce also keeps the workspace key itself off the ChaCha20 hot path.
 *
 * Envelope layout, stored verbatim in the `ciphertext` column:
 *
 *     [version:1][salt:16][ciphertext || poly1305 tag:16]
 */
class RecordCipher(private val random: SecureRandom = SecureRandom()) {

    fun encrypt(key: WorkspaceKey, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = deriveKey(key, salt)

        val cipher = ChaCha20Poly1305()
        cipher.init(true, AEADParameters(KeyParameter(derived), MAC_BITS, ZERO_NONCE, aad))

        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        val written = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        cipher.doFinal(out, written)
        derived.fill(0)

        return ByteArray(1 + SALT_BYTES + out.size).also { envelope ->
            envelope[0] = ENVELOPE_VERSION
            salt.copyInto(envelope, destinationOffset = 1)
            out.copyInto(envelope, destinationOffset = 1 + SALT_BYTES)
        }
    }

    fun decrypt(
        key: WorkspaceKey,
        envelope: ByteArray,
        aad: ByteArray = ByteArray(0),
    ): AppResult<ByteArray> {
        if (envelope.size < MIN_ENVELOPE_BYTES) {
            return AppResult.Failure(AppError.Crypto.DecryptionFailed)
        }
        if (envelope[0] != ENVELOPE_VERSION) {
            return AppResult.Failure(AppError.Crypto.UnsupportedEnvelopeVersion)
        }

        val salt = envelope.copyOfRange(1, 1 + SALT_BYTES)
        val body = envelope.copyOfRange(1 + SALT_BYTES, envelope.size)
        val derived = deriveKey(key, salt)

        return try {
            val cipher = ChaCha20Poly1305()
            cipher.init(false, AEADParameters(KeyParameter(derived), MAC_BITS, ZERO_NONCE, aad))
            val out = ByteArray(cipher.getOutputSize(body.size))
            val written = cipher.processBytes(body, 0, body.size, out, 0)
            val total = written + cipher.doFinal(out, written)
            AppResult.Success(if (total == out.size) out else out.copyOf(total))
        } catch (_: InvalidCipherTextException) {
            AppResult.Failure(AppError.Crypto.DecryptionFailed)
        } finally {
            derived.fill(0)
        }
    }

    private fun deriveKey(key: WorkspaceKey, salt: ByteArray): ByteArray {
        val ikm = key.bytes()
        return try {
            ByteArray(WorkspaceKey.SIZE_BYTES).also { derived ->
                HKDFBytesGenerator(SHA256Digest()).apply {
                    init(HKDFParameters(ikm, salt, HKDF_INFO))
                }.generateBytes(derived, 0, derived.size)
            }
        } finally {
            ikm.fill(0)
        }
    }

    private companion object {
        const val ENVELOPE_VERSION: Byte = 1
        const val SALT_BYTES = 16
        const val MAC_BITS = 128
        const val TAG_BYTES = MAC_BITS / 8
        const val MIN_ENVELOPE_BYTES = 1 + SALT_BYTES + TAG_BYTES

        val ZERO_NONCE = ByteArray(12)
        val HKDF_INFO = "oryareach:record:v1".toByteArray(Charsets.UTF_8)
    }
}
