package com.oryareach.feature.pairing

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Where this device is in getting a workspace and the key to read it.
 *
 * These are genuinely different screens rather than flags on one screen, because the device
 * can only be in one of them and each has its own irreversible step.
 */
@Immutable
sealed interface PairingStage {
    /** Reading stored state at launch. */
    data object Loading : PairingStage

    /** Signed in, no workspace yet. */
    data object Choose : PairingStage

    /**
     * A new workspace was created and its key generated. The phrase is shown exactly once,
     * and is the only way back in if both phones are lost.
     */
    data class ShowRecoveryPhrase(val words: List<String>) : PairingStage

    /** Entering the code the partner read out. */
    data object EnterCode : PairingStage

    /** Joined the workspace, but the partner has not yet released the key to this device. */
    data object AwaitingKey : PairingStage

    /** This device holds the key and can show an invitation for the partner. */
    data class Ready(
        val inviteCode: String? = null,
        val pendingDevices: List<PendingDevice> = emptyList(),
    ) : PairingStage
}

@Immutable
data class PendingDevice(
    val deviceKeyId: String,
    val label: String,
)

@Immutable
data class PairingUiState(
    val stage: PairingStage = PairingStage.Loading,
    val enteredCode: String = "",
    val phraseConfirmed: Boolean = false,
    val busy: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val canSubmitCode: Boolean get() = enteredCode.length == CODE_LENGTH && !busy

    companion object {
        const val CODE_LENGTH = 20
    }
}

sealed interface PairingEffect {
    /** The device now has a workspace and a key: the app proper can open. */
    data object Completed : PairingEffect
}
