package com.oryareach.app.di

import com.oryareach.core.crypto.WorkspaceKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the app knows about the current session: which workspace is open, and whether the
 * workspace key is unlocked.
 *
 * Held in memory only. The key is never written to disk in this form — the only persisted
 * copy is wrapped by an Android Keystore key, and it is dropped here whenever the app locks
 * so that a locked app genuinely cannot read its own data.
 */
class SessionState {

    private val _workspaceId = MutableStateFlow<String?>(null)
    val workspaceIdFlow: StateFlow<String?> = _workspaceId.asStateFlow()

    val workspaceId: String? get() = _workspaceId.value

    @Volatile
    private var key: WorkspaceKey? = null

    val isUnlocked: Boolean get() = key != null

    fun open(workspaceId: String, workspaceKey: WorkspaceKey) {
        _workspaceId.value = workspaceId
        key = workspaceKey
    }

    fun lock() {
        key?.destroy()
        key = null
    }

    fun signOut() {
        lock()
        _workspaceId.value = null
    }

    fun keyProvider(): com.oryareach.core.sync.WorkspaceKeyProvider =
        com.oryareach.core.sync.WorkspaceKeyProvider { key }
}
