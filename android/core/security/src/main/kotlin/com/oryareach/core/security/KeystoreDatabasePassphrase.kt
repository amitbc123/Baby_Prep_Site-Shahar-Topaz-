package com.oryareach.core.security

import android.content.Context
import android.util.Base64
import com.oryareach.core.database.DatabasePassphrase
import java.security.SecureRandom

/**
 * The SQLCipher passphrase: 32 random bytes, generated once per install and kept on disk only
 * as a Keystore-sealed blob.
 *
 * Losing it means the local database cannot be opened. That is recoverable — the data
 * re-syncs from the server — but anything not yet uploaded is gone, so the blob is never
 * regenerated silently once it exists.
 */
class KeystoreDatabasePassphrase(
    context: Context,
    private val random: SecureRandom = SecureRandom(),
) : DatabasePassphrase {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val box = KeystoreSealedBox(KEY_ALIAS)

    @Synchronized
    override fun get(): ByteArray {
        prefs.getString(PREF_SEALED, null)?.let { stored ->
            return box.unseal(Base64.decode(stored, Base64.NO_WRAP))
        }

        val passphrase = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        val sealed = Base64.encodeToString(box.seal(passphrase), Base64.NO_WRAP)
        // commit, not apply: if the process dies before this reaches disk, the next launch
        // would generate a different passphrase and the database would be unopenable.
        prefs.edit().putString(PREF_SEALED, sealed).commit()

        return passphrase
    }

    private companion object {
        const val PREFS = "sahar-secure"
        const val PREF_SEALED = "db-passphrase"
        const val KEY_ALIAS = "sahar-db-passphrase"
        const val PASSPHRASE_BYTES = 32
    }
}
