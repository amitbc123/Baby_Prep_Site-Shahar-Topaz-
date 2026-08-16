package com.oryareach.feature.conflicts

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.ConflictRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Stable
interface ConflictsActions {
    fun onKeepLocal(recordId: String)
    fun onKeepServer(recordId: String)
}

/**
 * One `koinViewModel()` per Activity lifetime, same as [com.oryareach.feature.update.UpdateViewModel]
 * — this is hosted at the app root (see `ConflictHost` in `:app`), not behind a tab, because a
 * conflict means a partner's edit is stuck unsynced until someone looks at it.
 */
class ConflictsViewModel(
    private val repository: ConflictRepository,
) : ViewModel(), ConflictsActions {

    private val _uiState = MutableStateFlow(ConflictsUiState())
    val uiState: StateFlow<ConflictsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeConflicts().collect { conflicts -> set { it.copy(conflicts = conflicts) } }
        }
    }

    override fun onKeepLocal(recordId: String) {
        viewModelScope.launch {
            set { it.copy(resolvingId = recordId) }
            repository.keepLocal(recordId)
            set { it.copy(resolvingId = null) }
        }
    }

    override fun onKeepServer(recordId: String) {
        viewModelScope.launch {
            set { it.copy(resolvingId = recordId) }
            repository.keepServer(recordId)
            set { it.copy(resolvingId = null) }
        }
    }

    private fun set(block: (ConflictsUiState) -> ConflictsUiState) {
        _uiState.value = block(_uiState.value)
    }
}
