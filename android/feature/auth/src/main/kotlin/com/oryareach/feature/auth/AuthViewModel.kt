package com.oryareach.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import com.oryareach.core.network.auth.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@androidx.compose.runtime.Stable
interface AuthActions {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onTogglePasswordVisibility()
    fun onRememberMeChange(value: Boolean)
    fun onModeChange(mode: AuthMode)
    fun onSubmit()
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel(), AuthActions {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Channel, not SharedFlow: an effect emitted while the screen is backgrounded buffers and
    // replays on resume instead of being dropped.
    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    override fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorMessage = null, infoMessage = null) }
    }

    override fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    override fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    override fun onRememberMeChange(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    override fun onModeChange(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    override fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(submitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.SignIn -> auth.signIn(state.email, state.password, state.rememberMe)
                AuthMode.SignUp -> auth.signUp(state.email, state.password)
            }

            when (result) {
                is AppResult.Success -> {
                    // Sign-up with email confirmation on creates the account but no session,
                    // so nothing on screen would change. Say so explicitly instead of leaving
                    // the user staring at a form that appears to have done nothing.
                    val awaitingConfirmation =
                        state.mode == AuthMode.SignUp && auth.currentUserId() == null

                    _uiState.update {
                        it.copy(
                            submitting = false,
                            password = "",
                            infoMessage = if (awaitingConfirmation) {
                                R.string.auth_confirmation_sent
                            } else {
                                null
                            },
                        )
                    }

                    if (!awaitingConfirmation) _effects.trySend(AuthEffect.SignedIn)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(submitting = false, errorMessage = result.error.toMessageRes())
                }
            }
        }
    }
}

private fun MutableStateFlow<AuthUiState>.update(block: (AuthUiState) -> AuthUiState) {
    value = block(value)
}

private fun AppError.toMessageRes(): Int = when (this) {
    is AppError.Network.Offline -> R.string.auth_error_offline
    is AppError.Network.Timeout -> R.string.auth_error_timeout
    // Deliberately the same message for a wrong password and an unknown address: telling the
    // two apart would confirm whether an account exists.
    is AppError.Network.Unauthorized -> R.string.auth_error_credentials
    is AppError.Network.Server -> if (status == 409) {
        R.string.auth_error_already_registered
    } else {
        R.string.auth_error_generic
    }
    else -> R.string.auth_error_generic
}
