package com.oryareach.feature.dates

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oryareach.core.model.ImportantDate
import kotlinx.datetime.LocalDate

@Immutable
data class DatesUiState(
    // Persisted snapshot: the live list from Room, already decrypted.
    val dates: List<ImportantDate> = emptyList(),

    // Editable input: the add/edit sheet's form.
    val editingId: String? = null,
    val formDate: LocalDate? = null,
    val formTitle: String = "",
    val formWish: String = "",

    // Transient UI-only: must not survive the screen.
    val sheetVisible: Boolean = false,
    val datePickerVisible: Boolean = false,
    val submitting: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val canSubmitForm: Boolean get() = formDate != null && formTitle.isNotBlank() && !submitting

    val isEditing: Boolean get() = editingId != null
}
