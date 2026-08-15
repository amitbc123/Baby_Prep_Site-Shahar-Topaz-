package com.oryareach.core.network.auth

import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Whether anyone is signed in on this device.
 *
 * `Unknown` is deliberately distinct from `SignedOut`: at cold start the SDK has not finished
 * loading the stored session, and treating that moment as signed-out would bounce the user to
 * the login screen on every launch.
 */
enum class AuthState { Unknown, SignedIn, SignedOut }

interface AuthRepository {
    val state: Flow<AuthState>
    fun currentUserId(): String?
    suspend fun signUp(email: String, password: String): AppResult<Unit>
    suspend fun signIn(email: String, password: String): AppResult<Unit>
    suspend fun signOut(): AppResult<Unit>
}

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val state: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> AuthState.SignedIn
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            else -> AuthState.Unknown
        }
    }

    override fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun signUp(email: String, password: String): AppResult<Unit> = attempt {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signIn(email: String, password: String): AppResult<Unit> = attempt {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut(): AppResult<Unit> = attempt { client.auth.signOut() }

    private inline fun attempt(block: () -> Unit): AppResult<Unit> = try {
        block()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(e.toAuthError())
    }
}

/**
 * Maps SDK failures to the domain error type. Credential problems are reported as
 * `Unauthorized` without echoing which part was wrong — telling a caller that an email exists
 * but the password is wrong is an account-enumeration oracle.
 */
internal fun Exception.toAuthError(): AppError {
    val text = message.orEmpty().lowercase()
    return when {
        this is java.net.UnknownHostException -> AppError.Network.Offline
        this is java.net.SocketTimeoutException -> AppError.Network.Timeout
        "invalid" in text || "credential" in text || "password" in text ->
            AppError.Network.Unauthorized
        "already registered" in text || "already exists" in text ->
            AppError.Network.Server(status = 409)
        else -> AppError.Unexpected(message ?: this::class.simpleName.orEmpty())
    }
}
