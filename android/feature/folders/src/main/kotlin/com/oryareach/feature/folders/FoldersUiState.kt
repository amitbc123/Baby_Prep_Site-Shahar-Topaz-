package com.oryareach.feature.folders

import androidx.compose.runtime.Immutable
import com.oryareach.core.model.Folder

@Immutable
data class FoldersUiState(
    // Persisted snapshot: the current folder's children, already decrypted.
    val children: List<Folder> = emptyList(),
    /** Ancestors of the current folder, root first. Empty means we are at the root. */
    val breadcrumb: List<Folder> = emptyList(),

    // Editable input: the create/rename dialogs.
    val formName: String = "",
    val renamingFolder: Folder? = null,

    // Transient UI-only.
    val createDialogVisible: Boolean = false,
    val deleteConfirmFolder: Folder? = null,
) {
    val currentParentId: String? get() = breadcrumb.lastOrNull()?.id
    val canSubmitCreate: Boolean get() = formName.isNotBlank()
    val canSubmitRename: Boolean get() = formName.isNotBlank()
}
