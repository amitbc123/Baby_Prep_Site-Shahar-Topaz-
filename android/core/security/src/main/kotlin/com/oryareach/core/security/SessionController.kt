package com.oryareach.core.security

/**
 * The lock/sign-out half of the app's session, exposed as an interface so a UI-layer module
 * like `:feature:settings` can trigger them without depending on `:app` (where the concrete
 * session type actually lives) — the same seam as `WorkspaceKeyProvider` for read access.
 */
interface SessionController {
    fun lock()
    fun signOut()
}

/**
 * Drops the local encrypted database so previously-synced content stops being readable on
 * this device after sign-out. Its own interface, not folded into [SessionController], because
 * the concrete implementation needs the Room database instance and `:core:security` must not
 * depend on `:core:database` — same cross-module seam shape as [SessionController] itself.
 *
 * Deliberately does not touch [KeystoreDatabasePassphrase]'s sealed blob: that passphrase is
 * Keystore-backed and independent of workspace pairing, so leaving it in place is harmless —
 * a fresh, empty encrypted database is created under it the next time the app opens.
 */
fun interface LocalDataWiper {
    /** Wipes local data and restarts the app process — Room/Koin singletons already handed
     * out through the object graph cannot be safely swapped out from under live collectors
     * mid-process, so a clean relaunch is the only way to guarantee nothing keeps reading the
     * database instance that is about to be deleted out from under it. */
    fun wipeAndRestart()
}
