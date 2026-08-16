package com.oryareach.feature.dates

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.ImportantDateRepository
import com.oryareach.core.model.ImportantDate
import com.oryareach.core.network.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Stable
interface DatesActions {
    fun onAddClick()
    fun onEditClick(date: ImportantDate)
    fun onDismissSheet()
    fun onOpenDatePicker()
    fun onDismissDatePicker()
    fun onDateChange(value: LocalDate)
    fun onTitleChange(value: String)
    fun onWishChange(value: String)
    fun onSubmit()
    fun onDelete(id: String)
    fun onRefresh()
}

class DatesViewModel(
    private val repository: ImportantDateRepository,
    private val auth: AuthRepository,
    private val syncEngine: com.oryareach.core.sync.SyncEngine,
    private val workspaceId: () -> String?,
) : ViewModel(), DatesActions {

    private val _uiState = MutableStateFlow(DatesUiState())
    val uiState: StateFlow<DatesUiState> = _uiState.asStateFlow()

    init {
        workspaceId()?.let { id ->
            viewModelScope.launch {
                repository.observeAll(id).collect { dates -> set { it.copy(dates = dates) } }
            }
        }
    }

    override fun onAddClick() = set {
        it.copy(sheetVisible = true, editingId = null, formDate = null, formTitle = "", formWish = "")
    }

    override fun onEditClick(date: ImportantDate) = set {
        it.copy(
            sheetVisible = true,
            editingId = date.id,
            formDate = date.date,
            formTitle = date.title,
            formWish = date.wish.orEmpty(),
        )
    }

    override fun onDismissSheet() = set { it.copy(sheetVisible = false) }
    override fun onOpenDatePicker() = set { it.copy(datePickerVisible = true) }
    override fun onDismissDatePicker() = set { it.copy(datePickerVisible = false) }
    override fun onDateChange(value: LocalDate) = set { it.copy(formDate = value, datePickerVisible = false) }
    override fun onTitleChange(value: String) = set { it.copy(formTitle = value) }
    override fun onWishChange(value: String) = set { it.copy(formWish = value) }

    override fun onSubmit() {
        val state = _uiState.value
        val workspace = workspaceId() ?: return
        val date = state.formDate ?: return
        if (!state.canSubmitForm) return
        set { it.copy(submitting = true) }

        viewModelScope.launch {
            val editingId = state.editingId
            if (editingId == null) {
                repository.create(
                    workspaceId = workspace,
                    userId = auth.currentUserId().orEmpty(),
                    date = date,
                    title = state.formTitle,
                    wish = state.formWish.ifBlank { null },
                )
            } else {
                repository.update(editingId, date, state.formTitle, state.formWish.ifBlank { null })
            }
            set { it.copy(submitting = false, sheetVisible = false) }
        }
    }

    override fun onDelete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    override fun onRefresh() {
        if (_uiState.value.refreshing) return
        set { it.copy(refreshing = true) }
        viewModelScope.launch {
            syncEngine.sync()
            set { it.copy(refreshing = false) }
        }
    }

    private fun set(block: (DatesUiState) -> DatesUiState) {
        _uiState.value = block(_uiState.value)
    }
}
