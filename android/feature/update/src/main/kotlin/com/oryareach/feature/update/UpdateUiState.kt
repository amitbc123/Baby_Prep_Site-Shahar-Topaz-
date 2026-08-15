package com.oryareach.feature.update

import androidx.compose.runtime.Immutable
import com.oryareach.core.update.ReleaseManifest

@Immutable
data class UpdateUiState(
    // Persisted snapshot: what the last check found.
    val availableManifest: ReleaseManifest? = null,
    val mandatory: Boolean = false,

    // Transient UI-only.
    val checking: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloading: Boolean = false,
    val installing: Boolean = false,
    val errorMessage: String? = null,
) {
    val visible: Boolean get() = availableManifest != null
    val downloadFraction: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

sealed interface UpdateEffect {
    data class OpenRelease(val url: String) : UpdateEffect
    data class LaunchInstallConfirmation(val intent: android.content.Intent) : UpdateEffect
}
