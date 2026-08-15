package com.oryareach.feature.cycle

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.CycleRepository
import com.oryareach.core.network.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Stable
interface CycleActions {
    fun onStartPeriod()
    fun onEndPeriod()
    fun onDelete(id: String)
}

/**
 * The workspace id is read once, same as [com.oryareach.feature.tasks.TasksViewModel]: the
 * app's routing already guarantees a paired, unlocked device by the time this screen can be
 * reached, and there is no in-app flow that changes the open workspace mid-session.
 */
class CycleViewModel(
    private val repository: CycleRepository,
    private val auth: AuthRepository,
    private val workspaceId: () -> String?,
) : ViewModel(), CycleActions {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    init {
        workspaceId()?.let { id ->
            viewModelScope.launch {
                combine(repository.observeOngoing(id), repository.observeAll(id)) { ongoing, history ->
                    ongoing to history
                }.collect { (ongoing, history) ->
                    _uiState.update { it.copy(ongoing = ongoing, history = history) }
                }
            }
        }
    }

    override fun onStartPeriod() {
        val workspace = workspaceId() ?: return
        if (_uiState.value.busy || _uiState.value.isPeriodOngoing) return
        set { it.copy(busy = true) }

        viewModelScope.launch {
            repository.startPeriod(
                workspaceId = workspace,
                userId = auth.currentUserId().orEmpty(),
                startDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            )
            set { it.copy(busy = false) }
        }
    }

    override fun onEndPeriod() {
        val ongoing = _uiState.value.ongoing ?: return
        if (_uiState.value.busy) return
        set { it.copy(busy = true) }

        viewModelScope.launch {
            repository.endPeriod(
                id = ongoing.id,
                endDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            )
            set { it.copy(busy = false) }
        }
    }

    override fun onDelete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    private fun set(block: (CycleUiState) -> CycleUiState) {
        _uiState.value = block(_uiState.value)
    }
}

private fun MutableStateFlow<CycleUiState>.update(block: (CycleUiState) -> CycleUiState) {
    value = block(value)
}
