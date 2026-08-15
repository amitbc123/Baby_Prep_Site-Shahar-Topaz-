package com.oryareach.feature.tasks

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.Task
import com.oryareach.core.model.TaskCategory

@Immutable
data class TasksUiState(
    // Persisted snapshot: the live list from Room, already decrypted.
    val tasks: List<Task> = emptyList(),

    // Editable input: the add/edit sheet's form.
    val editingId: String? = null,
    val formTitle: String = "",
    val formCategory: TaskCategory = TaskCategory.OTHER,
    val formPriority: Priority = Priority.NORMAL,
    val formAssignee: Assignee? = null,
    val formNote: String = "",

    // Transient UI-only: must not survive the screen.
    val sheetVisible: Boolean = false,
    val submitting: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    // Derived as a getter so it can never drift from the inputs it describes.
    val canSubmitForm: Boolean get() = formTitle.isNotBlank() && !submitting

    val isEditing: Boolean get() = editingId != null
}

sealed interface TasksEffect {
    data object SheetDismissed : TasksEffect
}
