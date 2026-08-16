package com.oryareach.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Seals arbitrary bytes with a key that never leaves the Android Keystore.
 *
 * The Keystore holds the wrapping key, not the secret itself: hardware-backed keys cannot be
 * exported or used as raw material for our own crypto, so the pattern is always to keep the
 * secret as a sealed blob on disk and unseal it through the Keystore when needed.
 *
 * Sealed layout: `[iv:12][ciphertext || GCM tag:16]`
 */
internal class KeystoreSealedBox(private val alias: String) {

    fun seal(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyOrCreate())

        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "unexpected GCM IV size ${iv.size}" }
        val body = cipher.doFinal(plaintext)

        return ByteArray(iv.size + body.size).also {
            iv.copyInto(it)
            body.copyInto(it, destinationOffset = iv.size)
        }
    }

    fun unseal(sealed: ByteArray): ByteArray {
        require(sealed.size > IV_BYTES) { "sealed blob is too short to contain an IV" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyOrCreate(),
            GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES),
        )
        return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }

    private fun keyOrCreate(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Deliberately not setUserAuthenticationRequired: the database passphrase is
                // needed by background sync, which runs with the screen locked. The biometric
                // gate belongs on the workspace key, not on this one.
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
