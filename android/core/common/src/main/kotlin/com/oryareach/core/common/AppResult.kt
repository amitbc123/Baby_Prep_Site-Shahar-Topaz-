package com.oryareach.core.common

/**
 * The repository boundary's return type. Platform exceptions are mapped to [AppError]
 * here and never leak past a repository.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    sealed interface Network : AppError {
        data object Offline : Network
        data object Timeout : Network
        data class Server(val status: Int) : Network
        data object Unauthorized : Network
    }

    sealed interface Local : AppError {
        data object DiskFull : Local
        data class Database(val cause: String) : Local
    }

    sealed interface Crypto : AppError {
        /** Ciphertext failed authentication: tampered, truncated, or wrong key. */
        data object DecryptionFailed : Crypto
        data object KeyUnavailable : Crypto
        data object UnsupportedEnvelopeVersion : Crypto
    }

    data class Unexpected(val cause: String) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> = also {
    if (this is AppResult.Failure) action(error)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
