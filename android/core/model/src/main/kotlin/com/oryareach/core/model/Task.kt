package com.oryareach.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class TaskCategory {
    HOME_PREP,
    DOCUMENTS_AND_INSURANCE,
    MEDICAL,
    HOSPITAL_BAG,
    OTHER,
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: Priority = Priority.NORMAL,
    val done: Boolean = false,
    val dueDate: LocalDate? = null,
    val assignee: Assignee? = null,
    val note: String? = null,
) {
    /**
     * Takes today as a parameter rather than reading a clock: a model that reads the current
     * time cannot be tested at a chosen date, and would make list rendering depend on when
     * it happened to run.
     */
    fun isOverdue(today: LocalDate): Boolean = !done && dueDate != null && dueDate < today
}
