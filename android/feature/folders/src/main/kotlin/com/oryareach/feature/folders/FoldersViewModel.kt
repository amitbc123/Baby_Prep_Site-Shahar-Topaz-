package com.oryareach.feature.folders

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryareach.core.database.repository.FolderRepository
import com.oryareach.core.model.Folder
import com.oryareach.core.network.auth.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@Stable
interface FoldersActions {
    fun onOpenFolder(folder: Folder)
    fun onNavigateUp()
    fun onNavigateToBreadcrumb(index: Int)
    fun onAddClick()
    fun onDismissCreateDialog()
    fun onNameChange(value: String)
    fun onCreateSubmit()
    fun onRenameClick(folder: Folder)
    fun onRenameChange(value: String)
    fun onRenameSubmit()
    fun onDismissRename()
    fun onDeleteClick(folder: Folder)
    fun onConfirmDelete()
    fun onDismissDelete()
}

/**
 * The workspace id is read once, same as every other tab's ViewModel: routing already
 * guarantees a paired, unlocked device by the time this screen is reachable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoldersViewModel(
    private val repository: FolderRepository,
    private val auth: AuthRepository,
    private val workspaceId: () -> String?,
) : ViewModel(), FoldersActions {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    /** Kept separate from [_uiState] so editing the create/rename form never re-triggers
     * [FolderRepository.observeChildren] — only an actual navigation should. */
    private val currentParentId = MutableStateFlow<String?>(null)

    init {
        workspaceId()?.let { id ->
            viewModelScope.launch {
                currentParentId.flatMapLatest { parentId -> repository.observeChildren(id, parentId) }
                    .collect { children -> set { it.copy(children = children) } }
            }
        }
    }

    override fun onOpenFolder(folder: Folder) = navigateTo(_uiState.value.breadcrumb + folder)

    override fun onNavigateUp() = navigateTo(_uiState.value.breadcrumb.dropLast(1))

    override fun onNavigateToBreadcrumb(index: Int) = navigateTo(
        if (index < 0) emptyList() else _uiState.value.breadcrumb.take(index + 1),
    )

    private fun navigateTo(breadcrumb: List<Folder>) {
        set { it.copy(breadcrumb = breadcrumb) }
        currentParentId.value = breadcrumb.lastOrNull()?.id
    }

    override fun onAddClick() = set { it.copy(createDialogVisible = true, formName = "") }
    override fun onDismissCreateDialog() = set { it.copy(createDialogVisible = false) }
    override fun onNameChange(value: String) = set { it.copy(formName = value) }

    override fun onCreateSubmit() {
        val state = _uiState.value
        val workspace = workspaceId() ?: return
        if (!state.canSubmitCreate) return

        viewModelScope.launch {
            repository.create(workspace, auth.currentUserId().orEmpty(), state.formName.trim(), state.currentParentId)
            set { it.copy(createDialogVisible = false, formName = "") }
        }
    }

    override fun onRenameClick(folder: Folder) = set { it.copy(renamingFolder = folder, formName = folder.name) }
    override fun onRenameChange(value: String) = set { it.copy(formName = value) }
    override fun onDismissRename() = set { it.copy(renamingFolder = null) }

    override fun onRenameSubmit() {
        val state = _uiState.value
        val folder = state.renamingFolder ?: return
        if (!state.canSubmitRename) return

        viewModelScope.launch {
            repository.rename(folder.id, state.formName.trim())
            set { it.copy(renamingFolder = null) }
        }
    }

    override fun onDeleteClick(folder: Folder) = set { it.copy(deleteConfirmFolder = folder) }
    override fun onDismissDelete() = set { it.copy(deleteConfirmFolder = null) }

    override fun onConfirmDelete() {
        val workspace = workspaceId() ?: return
        val folder = _uiState.value.deleteConfirmFolder ?: return

        viewModelScope.launch {
            repository.delete(workspace, folder.id)
            set { it.copy(deleteConfirmFolder = null) }
        }
    }

    private fun set(block: (FoldersUiState) -> FoldersUiState) {
        _uiState.value = block(_uiState.value)
    }
}
