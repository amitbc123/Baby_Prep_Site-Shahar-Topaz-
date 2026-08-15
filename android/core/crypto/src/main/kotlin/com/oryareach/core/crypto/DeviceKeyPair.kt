package com.oryareach.core.crypto

/**
 * A device's X25519 identity, used to receive the workspace key when it joins the couple.
 *
 * Held as serialized bytes rather than Bouncy Castle types so the storage layer can persist
 * it without depending on the crypto library. The private half is only ever written to disk
 * wrapped by an Android Keystore key.
 */
class DeviceKeyPair(
    val publicKey: ByteArray,
    private val privateKey: ByteArray,
) {
    init {
        require(publicKey.size == KEY_BYTES) { "public key must be $KEY_BYTES bytes" }
        require(privateKey.size == KEY_BYTES) { "private key must be $KEY_BYTES bytes" }
    }

    fun privateKeyBytes(): ByteArray = privateKey.copyOf()

    override fun toString(): String = "DeviceKeyPair(public=${publicKey.size}B, private=redacted)"

    companion object {
        const val KEY_BYTES = 32
    }
}
