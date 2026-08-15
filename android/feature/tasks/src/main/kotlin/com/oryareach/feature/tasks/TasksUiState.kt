package com.oryareach.feature.tasks

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Document
import com.oryareach.core.model.Priority
import com.oryareach.core.model.Recurrence
import com.oryareach.core.model.Task
import com.oryareach.core.model.TaskCategory
import kotlinx.datetime.LocalDate

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
    val formDueDate: LocalDate? = null,
    val formRecurrence: Recurrence? = null,
    val formTags: List<String> = emptyList(),
    val formTagInput: String = "",

    // Attachments — only meaningful while editing an existing task (a task must exist before
    // anything can be attached to it).
    val attachments: List<Document> = emptyList(),
    val attaching: Boolean = false,

    // Filters the task list; null means "all tags".
    val activeTagFilter: String? = null,

    // Transient UI-only: must not survive the screen.
    val sheetVisible: Boolean = false,
    val submitting: Boolean = false,
    val seedingHospitalBag: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    // Derived as a getter so it can never drift from the inputs it describes.
    val canSubmitForm: Boolean get() = formTitle.isNotBlank() && !submitting

    val isEditing: Boolean get() = editingId != null

    val allTags: List<String> get() = tasks.flatMap { it.tags }.distinct().sorted()

    val visibleTasks: List<Task>
        get() = activeTagFilter?.let { tag -> tasks.filter { tag in it.tags } } ?: tasks
}

sealed interface TasksEffect {
    data object SheetDismissed : TasksEffect
}
