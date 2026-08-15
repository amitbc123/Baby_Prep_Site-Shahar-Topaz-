package com.oryareach.core.security

import android.content.Context
import android.util.Base64

/**
 * Small key/value store whose values are sealed by a hardware-backed Keystore key.
 *
 * Used for anything that must survive a restart but must not sit on disk in the clear:
 * auth tokens, the device's private key, the wrapped workspace key. SharedPreferences holds
 * only ciphertext, so an ADB backup or a pulled data directory yields nothing usable.
 */
class KeystoreBlobStore(context: Context, name: String = DEFAULT_NAME) {

    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val box = KeystoreSealedBox(KEY_ALIAS)

    fun put(key: String, value: ByteArray) {
        val sealed = Base64.encodeToString(box.seal(value), Base64.NO_WRAP)
        prefs.edit().putString(key, sealed).commit()
    }

    fun get(key: String): ByteArray? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching { box.unseal(Base64.decode(stored, Base64.NO_WRAP)) }
            // A blob that will not unseal means the Keystore key is gone — app data cleared,
            // device restored to another handset, or biometric enrolment reset. Treat it as
            // absent so the user is asked to sign in again rather than seeing a crash.
            .getOrNull()
    }

    fun putString(key: String, value: String) = put(key, value.toByteArray(Charsets.UTF_8))

    fun getString(key: String): String? = get(key)?.toString(Charsets.UTF_8)

    fun remove(key: String) {
        prefs.edit().remove(key).commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val DEFAULT_NAME = "sahar-secure"
        const val KEY_ALIAS = "sahar-blob-store"
    }
}
