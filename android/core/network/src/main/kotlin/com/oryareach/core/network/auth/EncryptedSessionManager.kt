package com.oryareach.core.network.auth

import com.oryareach.core.security.KeystoreBlobStore
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/**
 * Persists the Supabase session sealed by the Android Keystore.
 *
 * The SDK's default manager writes the session to plain SharedPreferences, which would leave
 * a live refresh token readable in the app's data directory. A refresh token is enough to
 * mint new access tokens indefinitely, so it gets the same protection as the rest.
 */
class EncryptedSessionManager(
    private val store: KeystoreBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionManager {

    override suspend fun saveSession(session: UserSession) {
        store.putString(KEY, json.encodeToString(session))
    }

    /**
     * The SDK declares this non-null and funnels "no session" through [loadSessionOrNull],
     * whose default implementation catches whatever this throws. Both are overridden so the
     * absent case is explicit either way.
     */
    override suspend fun loadSession(): UserSession =
        requireNotNull(loadSessionOrNull()) { "no stored session" }

    override suspend fun loadSessionOrNull(): UserSession? {
        val raw = store.getString(KEY) ?: return null
        // A session that will not parse (SDK format change, corrupted blob) means signing in
        // again, which is recoverable; propagating the error would wedge app startup.
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        store.remove(KEY)
    }

    private companion object {
        const val KEY = "supabase-session"
    }
}
