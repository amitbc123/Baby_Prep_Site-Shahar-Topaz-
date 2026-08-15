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
