package com.oryareach.feature.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val biometricUnlockEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val screenshotsBlocked: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val recoveryPhrase: List<String>? = null,
    val busy: Boolean = false,
) {
    val autoLockOptionMinutes: List<Int> get() = listOf(1, 5, 15, 30)
}

sealed interface SettingsEffect {
    /** Handled in `:app`, which is the only place both `:feature:settings` and
     * `:feature:pairing` are visible — feature modules must not depend on each other. */
    data object NavigateToDeviceManagement : SettingsEffect

    /** Handled in `:app`: launches the system notification-permission prompt on Android 13+.
     * The result comes back via [SettingsActions.onNotificationPermissionResult]. */
    data object RequestNotificationPermission : SettingsEffect
}
