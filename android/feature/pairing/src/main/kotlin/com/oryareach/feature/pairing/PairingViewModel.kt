package com.oryareach.feature.pairing

import android.os.Build
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.common.AppError
import com.oryareach.core.common.AppResult
import com.oryareach.core.crypto.KeyWrap
import com.oryareach.core.crypto.RecoveryPhrase
import com.oryareach.core.crypto.WorkspaceKey
import com.oryareach.core.network.workspace.WorkspaceRepository
import com.oryareach.core.security.DeviceIdentity
import com.oryareach.core.security.InvitationToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Stable
interface PairingActions {
    fun onCreateWorkspace()
    fun onChooseJoin()
    fun onCodeChange(value: String)
    fun onSubmitCode()
    fun onPhraseConfirmedChange(confirmed: Boolean)
    fun onFinishRecoveryPhrase()
    fun onGenerateInvite()
    fun onApproveDevice(deviceKeyId: String)
    fun onRefresh()
}

/**
 * Drives getting this device into a workspace with a usable key.
 *
 * The key never travels through the invitation. The code only proves membership; the key
 * itself is sealed to the joining device's public key by the partner's device, which is why
 * joining ends in [PairingStage.AwaitingKey] until the other phone approves.
 */
class PairingViewModel(
    private val workspaces: WorkspaceRepository,
    private val identity: DeviceIdentity,
    private val keyWrap: KeyWrap = KeyWrap(),
    private val onWorkspaceOpened: (String, WorkspaceKey) -> Unit,
) : ViewModel(), PairingActions {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PairingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Held until the phrase is confirmed, so it is never persisted before the user sees it. */
    private var pendingKey: WorkspaceKey? = null

    init {
        onRefresh()
    }

    override fun onRefresh() {
        viewModelScope.launch {
            val storedWorkspace = identity.workspaceId
            val storedKey = identity.workspaceKey()

            if (storedWorkspace != null && storedKey != null) {
                onWorkspaceOpened(storedWorkspace, storedKey)
                showReady(storedWorkspace)
                return@launch
            }

            when (val remote = workspaces.currentWorkspaceId()) {
                is AppResult.Failure -> fail(remote.error)
                is AppResult.Success -> {
                    val workspaceId = remote.data
                    when {
                        workspaceId == null -> set { it.copy(stage = PairingStage.Choose) }
                        // A member without a key is a joiner waiting for the partner to release it.
                        else -> tryClaimKey(workspaceId)
                    }
                }
            }
        }
    }

    override fun onCreateWorkspace() {
        if (_uiState.value.busy) return
        busy(true)

        viewModelScope.launch {
            when (val created = workspaces.createWorkspace()) {
                is AppResult.Failure -> fail(created.error)
                is AppResult.Success -> {
                    val workspaceId = created.data
                    val key = WorkspaceKey.generate()
                    pendingKey = key

                    when (val registered = registerDevice(workspaceId)) {
                        is AppResult.Failure -> fail(registered.error)
                        is AppResult.Success -> {
                            // Seal the key to this device too, so a reinstall on the same
                            // account can recover it without the partner being involved.
                            val blob = keyWrap.wrap(key, identity.keyPair().publicKey)
                            when (val upload = workspaces.uploadWrappedKey(workspaceId, registered.data, blob)) {
                                is AppResult.Failure -> fail(upload.error)
                                is AppResult.Success -> {
                                    identity.workspaceId = workspaceId
                                    set {
                                        it.copy(
                                            busy = false,
                                            phraseConfirmed = false,
                                            stage = PairingStage.ShowRecoveryPhrase(
                                                RecoveryPhrase.encode(key),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onFinishRecoveryPhrase() {
        val key = pendingKey ?: return
        val workspaceId = identity.workspaceId ?: return
        if (!_uiState.value.phraseConfirmed) return

        identity.saveWorkspaceKey(key)
        pendingKey = null
        onWorkspaceOpened(workspaceId, key)
        showReady(workspaceId)
        _effects.trySend(PairingEffect.Completed)
    }

    override fun onChooseJoin() = set { it.copy(stage = PairingStage.EnterCode, errorMessage = null) }

    override fun onCodeChange(value: String) {
        set { it.copy(enteredCode = InvitationToken.normalize(value), errorMessage = null) }
    }

    override fun onSubmitCode() {
        val code = _uiState.value.enteredCode
        if (!InvitationToken.isWellFormed(code) || _uiState.value.busy) return
        busy(true)

        viewModelScope.launch {
            when (val accepted = workspaces.acceptInvitation(code)) {
                is AppResult.Failure -> fail(accepted.error)
                is AppResult.Success -> {
                    val workspaceId = accepted.data
                    identity.workspaceId = workspaceId
                    when (val registered = registerDevice(workspaceId)) {
                        is AppResult.Failure -> fail(registered.error)
                        is AppResult.Success -> set {
                            it.copy(busy = false, stage = PairingStage.AwaitingKey)
                        }
                    }
                }
            }
        }
    }

    override fun onPhraseConfirmedChange(confirmed: Boolean) =
        set { it.copy(phraseConfirmed = confirmed) }

    override fun onGenerateInvite() {
        if (_uiState.value.busy) return
        busy(true)

        viewModelScope.launch {
            val token = InvitationToken.generate()
            when (val created = workspaces.createInvitation(token, expiresInHours = INVITE_HOURS)) {
                is AppResult.Failure -> fail(created.error)
                is AppResult.Success -> set { state ->
                    val stage = state.stage as? PairingStage.Ready ?: PairingStage.Ready()
                    state.copy(busy = false, stage = stage.copy(inviteCode = token))
                }
            }
        }
    }

    /**
     * Releases the workspace key to a partner device, sealed to its public key. This is the
     * only moment the key leaves this phone, and it always requires a deliberate tap.
     */
    override fun onApproveDevice(deviceKeyId: String) {
        val workspaceId = identity.workspaceId ?: return
        val key = identity.workspaceKey() ?: return
        if (_uiState.value.busy) return
        busy(true)

        viewModelScope.launch {
            when (val devices = workspaces.devices(workspaceId)) {
                is AppResult.Failure -> fail(devices.error)
                is AppResult.Success -> {
                    val target = devices.data.firstOrNull { it.deviceKeyId == deviceKeyId }
                    if (target == null) {
                        fail(AppError.Unexpected("device disappeared"))
                        return@launch
                    }

                    val blob = keyWrap.wrap(key, target.publicKey)
                    when (val upload = workspaces.uploadWrappedKey(workspaceId, deviceKeyId, blob)) {
                        is AppResult.Failure -> fail(upload.error)
                        is AppResult.Success -> {
                            busy(false)
                            showReady(workspaceId)
                        }
                    }
                }
            }
        }
    }

    /** For a device that has joined but not yet been handed the key. */
    private suspend fun tryClaimKey(workspaceId: String) {
        val deviceKeyId = identity.registeredKeyId
            ?: when (val registered = registerDevice(workspaceId)) {
                is AppResult.Failure -> return fail(registered.error)
                is AppResult.Success -> registered.data
            }

        when (val wrapped = workspaces.wrappedKeyFor(deviceKeyId)) {
            is AppResult.Failure -> fail(wrapped.error)
            is AppResult.Success -> {
                val blob = wrapped.data
                if (blob == null) {
                    set { it.copy(stage = PairingStage.AwaitingKey, busy = false) }
                    return
                }

                when (val opened = keyWrap.unwrap(blob, identity.keyPair())) {
                    is AppResult.Failure -> fail(opened.error)
                    is AppResult.Success -> {
                        identity.saveWorkspaceKey(opened.data)
                        identity.workspaceId = workspaceId
                        onWorkspaceOpened(workspaceId, opened.data)
                        showReady(workspaceId)
                        _effects.trySend(PairingEffect.Completed)
                    }
                }
            }
        }
    }

    private suspend fun registerDevice(workspaceId: String): AppResult<String> {
        identity.registeredKeyId?.let { return AppResult.Success(it) }

        val result = workspaces.publishDeviceKey(
            workspaceId = workspaceId,
            publicKey = identity.keyPair().publicKey,
            label = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
        if (result is AppResult.Success) identity.registeredKeyId = result.data
        return result
    }

    private fun showReady(workspaceId: String) {
        viewModelScope.launch {
            val pending = when (val devices = workspaces.devices(workspaceId)) {
                is AppResult.Failure -> emptyList()
                is AppResult.Success -> devices.data
                    .filterNot { it.hasWrappedKey }
                    .map { PendingDevice(it.deviceKeyId, it.label.orEmpty()) }
            }

            set { state ->
                val existing = state.stage as? PairingStage.Ready
                state.copy(
                    busy = false,
                    stage = PairingStage.Ready(
                        inviteCode = existing?.inviteCode,
                        pendingDevices = pending,
                    ),
                )
            }
        }
    }

    private fun busy(value: Boolean) = set { it.copy(busy = value, errorMessage = null) }

    private fun fail(error: AppError) = set {
        it.copy(busy = false, errorMessage = error.toMessageRes())
    }

    private fun set(block: (PairingUiState) -> PairingUiState) {
        _uiState.value = block(_uiState.value)
    }

    private companion object {
        const val INVITE_HOURS = 24
    }
}

private fun AppError.toMessageRes(): Int = when (this) {
    is AppError.Network.Offline -> R.string.pairing_error_offline
    is AppError.Network.Timeout -> R.string.pairing_error_timeout
    is AppError.Network.Server -> when (status) {
        422 -> R.string.pairing_error_invalid_code
        409 -> R.string.pairing_error_workspace_full
        else -> R.string.pairing_error_generic
    }
    is AppError.Crypto.DecryptionFailed -> R.string.pairing_error_key_mismatch
    else -> R.string.pairing_error_generic
}
