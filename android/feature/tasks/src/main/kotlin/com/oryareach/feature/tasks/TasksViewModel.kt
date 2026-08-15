package com.oryareach.feature.tasks

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.TaskRepository
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.Task
import com.oryareach.core.model.TaskCategory
import com.oryareach.core.network.auth.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Stable
interface TasksActions {
    fun onAddClick()
    fun onEditClick(task: Task)
    fun onDismissSheet()
    fun onTitleChange(value: String)
    fun onCategoryChange(value: TaskCategory)
    fun onPriorityChange(value: Priority)
    fun onAssigneeChange(value: Assignee?)
    fun onNoteChange(value: String)
    fun onSubmit()
    fun onToggleDone(id: String)
    fun onDelete(id: String)
    fun onSeedHospitalBag(titles: List<String>)
}

/**
 * The workspace id is read once: by the time this screen can be reached, the app's routing
 * has already confirmed the device is paired and unlocked, and there is no in-app flow that
 * changes the open workspace without a process restart.
 */
class TasksViewModel(
    private val repository: TaskRepository,
    private val auth: AuthRepository,
    private val workspaceId: () -> String?,
) : ViewModel(), TasksActions {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TasksEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        workspaceId()?.let { id ->
            viewModelScope.launch {
                repository.observeAll(id).collect { list ->
                    _uiState.update { it.copy(tasks = list) }
                }
            }
        }
    }

    override fun onAddClick() = set {
        TasksUiState(tasks = it.tasks, sheetVisible = true)
    }

    override fun onEditClick(task: Task) = set {
        it.copy(
            editingId = task.id,
            formTitle = task.title,
            formCategory = task.category,
            formPriority = task.priority,
            formAssignee = task.assignee,
            formNote = task.note.orEmpty(),
            sheetVisible = true,
            errorMessage = null,
        )
    }

    override fun onDismissSheet() {
        set { TasksUiState(tasks = it.tasks) }
        _effects.trySend(TasksEffect.SheetDismissed)
    }

    override fun onTitleChange(value: String) = set { it.copy(formTitle = value, errorMessage = null) }
    override fun onCategoryChange(value: TaskCategory) = set { it.copy(formCategory = value) }
    override fun onPriorityChange(value: Priority) = set { it.copy(formPriority = value) }
    override fun onAssigneeChange(value: Assignee?) = set { it.copy(formAssignee = value) }
    override fun onNoteChange(value: String) = set { it.copy(formNote = value) }

    override fun onSubmit() {
        val state = _uiState.value
        val workspace = workspaceId() ?: return
        if (!state.canSubmitForm) return

        set { it.copy(submitting = true) }

        viewModelScope.launch {
            val note = state.formNote.ifBlank { null }
            if (state.editingId != null) {
                repository.update(
                    id = state.editingId,
                    title = state.formTitle.trim(),
                    category = state.formCategory,
                    priority = state.formPriority,
                    assignee = state.formAssignee,
                    note = note,
                )
            } else {
                repository.create(
                    workspaceId = workspace,
                    userId = auth.currentUserId().orEmpty(),
                    title = state.formTitle.trim(),
                    category = state.formCategory,
                    priority = state.formPriority,
                    assignee = state.formAssignee,
                    note = note,
                )
            }
            set { TasksUiState(tasks = it.tasks) }
            _effects.trySend(TasksEffect.SheetDismissed)
        }
    }

    override fun onToggleDone(id: String) {
        viewModelScope.launch { repository.toggleDone(id) }
    }

    override fun onDelete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /**
     * Additive and re-runnable, like the web app's preset: only titles not already present
     * (in any category — someone may have moved one) are created, so tapping this twice
     * never duplicates the checklist.
     */
    override fun onSeedHospitalBag(titles: List<String>) {
        val workspace = workspaceId() ?: return
        if (_uiState.value.seedingHospitalBag) return
        set { it.copy(seedingHospitalBag = true) }

        viewModelScope.launch {
            val existingTitles = _uiState.value.tasks.map { it.title.trim().lowercase() }.toSet()
            val userId = auth.currentUserId().orEmpty()
            titles.filter { it.trim().lowercase() !in existingTitles }.forEach { title ->
                repository.create(
                    workspaceId = workspace,
                    userId = userId,
                    title = title,
                    category = TaskCategory.HOSPITAL_BAG,
                )
            }
            set { it.copy(seedingHospitalBag = false) }
        }
    }

    private fun set(block: (TasksUiState) -> TasksUiState) {
        _uiState.value = block(_uiState.value)
    }
}

private fun MutableStateFlow<TasksUiState>.update(block: (TasksUiState) -> TasksUiState) {
    value = block(value)
}
