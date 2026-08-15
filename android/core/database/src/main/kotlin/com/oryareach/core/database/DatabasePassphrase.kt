package com.oryareach.core.database

/**
 * Supplies the SQLCipher passphrase.
 *
 * An interface rather than a direct Keystore call so this module stays free of Keystore
 * details and can be exercised in tests with a fixed passphrase. The real implementation
 * lives in `:core:security`, where the passphrase is generated once and sealed by a
 * hardware-backed Keystore key.
 */
fun interface DatabasePassphrase {
    /**
     * Returns the raw passphrase bytes. The caller zeroes the array after use, so this must
     * return a fresh copy every time rather than a shared buffer.
     */
    fun get(): ByteArray
}
