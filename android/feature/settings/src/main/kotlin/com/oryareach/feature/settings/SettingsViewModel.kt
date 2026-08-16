package com.oryareach.feature.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.crypto.RecoveryPhrase
import com.oryareach.core.network.auth.AuthRepository
import com.oryareach.core.security.DeviceIdentity
import com.oryareach.core.security.LocalDataWiper
import com.oryareach.core.security.SessionController
import com.oryareach.core.settings.ReminderScheduler
import com.oryareach.core.settings.SettingsPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Stable
interface SettingsActions {
    fun onBiometricToggle(enabled: Boolean)
    fun onAutoLockMinutesChange(minutes: Int)
    fun onScreenshotsToggle(blocked: Boolean)
    fun onNotificationsToggle(enabled: Boolean)
    fun onNotificationPermissionResult(granted: Boolean)
    fun onLockNowClick()
    fun onShowRecoveryPhraseClick()
    fun onDismissRecoveryPhrase()
    fun onManageDevicesClick()
    fun onSignOutClick()
}

class SettingsViewModel(
    private val preferences: SettingsPreferences,
    private val reminders: ReminderScheduler,
    private val identity: DeviceIdentity,
    private val session: SessionController,
    private val auth: AuthRepository,
    private val localDataWiper: LocalDataWiper,
) : ViewModel(), SettingsActions {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.biometricUnlockEnabled,
                preferences.autoLockTimeoutMinutes,
                preferences.screenshotsBlocked,
                preferences.notificationsEnabled,
            ) { biometric, autoLock, screenshots, notifications ->
                Preferences(biometric, autoLock, screenshots, notifications)
            }.collect { prefs ->
                set {
                    it.copy(
                        biometricUnlockEnabled = prefs.biometric,
                        autoLockTimeoutMinutes = prefs.autoLock,
                        screenshotsBlocked = prefs.screenshots,
                        notificationsEnabled = prefs.notifications,
                    )
                }
            }
        }
    }

    override fun onBiometricToggle(enabled: Boolean) {
        viewModelScope.launch { preferences.setBiometricUnlockEnabled(enabled) }
    }

    override fun onAutoLockMinutesChange(minutes: Int) {
        viewModelScope.launch { preferences.setAutoLockTimeoutMinutes(minutes) }
    }

    override fun onScreenshotsToggle(blocked: Boolean) {
        viewModelScope.launch { preferences.setScreenshotsBlocked(blocked) }
    }

    override fun onNotificationsToggle(enabled: Boolean) {
        if (enabled) {
            _effects.trySend(SettingsEffect.RequestNotificationPermission)
        } else {
            viewModelScope.launch { preferences.setNotificationsEnabled(false) }
            reminders.cancel()
        }
    }

    override fun onNotificationPermissionResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch { preferences.setNotificationsEnabled(true) }
        reminders.schedule()
    }

    override fun onLockNowClick() {
        if (!_uiState.value.biometricUnlockEnabled) return
        session.lock()
    }

    override fun onShowRecoveryPhraseClick() {
        val key = identity.workspaceKey() ?: return
        set { it.copy(recoveryPhrase = RecoveryPhrase.encode(key)) }
    }

    override fun onDismissRecoveryPhrase() = set { it.copy(recoveryPhrase = null) }

    override fun onManageDevicesClick() {
        _effects.trySend(SettingsEffect.NavigateToDeviceManagement)
    }

    override fun onSignOutClick() {
        if (_uiState.value.busy) return
        set { it.copy(busy = true) }

        viewModelScope.launch {
            auth.signOut()
            identity.forget()
            session.signOut()
            // Restarts the process — nothing after this point runs; see LocalDataWiper's
            // doc comment for why the database can't just be swapped out in place.
            localDataWiper.wipeAndRestart()
        }
    }

    private fun set(block: (SettingsUiState) -> SettingsUiState) {
        _uiState.value = block(_uiState.value)
    }

    private data class Preferences(
        val biometric: Boolean,
        val autoLock: Int,
        val screenshots: Boolean,
        val notifications: Boolean,
    )
}
