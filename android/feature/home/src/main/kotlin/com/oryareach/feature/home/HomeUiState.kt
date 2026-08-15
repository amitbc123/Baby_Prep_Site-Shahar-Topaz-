package com.oryareach.feature.home

import androidx.compose.runtime.Immutable
import com.oryareach.core.domain.pregnancy.PregnancyProgress
import kotlinx.datetime.LocalDate

@Immutable
data class HomeUiState(
    // Persisted snapshot: what's actually stored.
    val dueDate: LocalDate? = null,
    val babyName: String? = null,
    val openTaskCount: Int = 0,
    val budgetEstimated: Int = 0,
    val budgetSpent: Int = 0,

    // Derived from dueDate — a getter, never stored, so it can never go stale.
    val progress: PregnancyProgress? = null,

    // Editable input: the due-date/name edit sheet.
    val editingDueDate: LocalDate? = null,
    val editingBabyName: String = "",

    // Transient UI-only.
    val sheetVisible: Boolean = false,
    val datePickerVisible: Boolean = false,
    val importing: Boolean = false,
    val importResult: ImportResult? = null,
) {
    val hasDueDate: Boolean get() = dueDate != null
    val canSubmitForm: Boolean get() = editingDueDate != null
}

sealed interface ImportResult {
    data class Success(val taskCount: Int, val shoppingCount: Int, val dateCount: Int) : ImportResult
    data object InvalidFile : ImportResult
}
