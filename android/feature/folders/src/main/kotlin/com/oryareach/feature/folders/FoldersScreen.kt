package com.oryareach.feature.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.model.Folder as FolderModel
import com.oryareach.core.ui.theme.OrYareachTheme

@Composable
fun FoldersScreen(
    uiState: FoldersUiState,
    actions: FoldersActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = actions::onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.folders_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Breadcrumb(uiState = uiState, actions = actions)

            if (uiState.children.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.folders_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.children, key = FolderModel::id) { folder ->
                        FolderRow(folder = folder, actions = actions)
                    }
                }
            }
        }
    }

    if (uiState.createDialogVisible) {
        NameDialog(
            title = stringResource(R.string.folders_new_title),
            value = uiState.formName,
            onValueChange = actions::onNameChange,
            onConfirm = actions::onCreateSubmit,
            onDismiss = actions::onDismissCreateDialog,
            confirmEnabled = uiState.canSubmitCreate,
        )
    }

    uiState.renamingFolder?.let {
        NameDialog(
            title = stringResource(R.string.folders_rename_title),
            value = uiState.formName,
            onValueChange = actions::onRenameChange,
            onConfirm = actions::onRenameSubmit,
            onDismiss = actions::onDismissRename,
            confirmEnabled = uiState.canSubmitRename,
        )
    }

    uiState.deleteConfirmFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = actions::onDismissDelete,
            title = { Text(stringResource(R.string.folders_delete_title)) },
            text = { Text(stringResource(R.string.folders_delete_body, folder.name)) },
            confirmButton = {
                TextButton(onClick = actions::onConfirmDelete) { Text(stringResource(R.string.folders_delete)) }
            },
            dismissButton = {
                TextButton(onClick = actions::onDismissDelete) { Text(stringResource(R.string.folders_cancel)) }
            },
        )
    }
}

@Composable
private fun Breadcrumb(uiState: FoldersUiState, actions: FoldersActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.folders_root),
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.breadcrumb.isEmpty()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.clickable { actions.onNavigateToBreadcrumb(-1) },
        )
        uiState.breadcrumb.forEachIndexed { index, folder ->
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.padding(horizontal = 2.dp))
            val isLast = index == uiState.breadcrumb.lastIndex
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { actions.onNavigateToBreadcrumb(index) },
            )
        }
    }
}

@Composable
private fun FolderRow(folder: FolderModel, actions: FoldersActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 12.dp).clickable { actions.onOpenFolder(folder) },
            )
            IconButton(onClick = { actions.onRenameClick(folder) }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.folders_rename_title))
            }
            IconButton(onClick = { actions.onDeleteClick(folder) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.folders_delete))
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.folders_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(stringResource(R.string.folders_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.folders_cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun FoldersPreview() {
    OrYareachTheme {
        FoldersScreen(
            uiState = FoldersUiState(children = listOf(FolderModel(id = "1", name = "Documents", path = "/1/"))),
            actions = NoopFoldersActions,
        )
    }
}

private object NoopFoldersActions : FoldersActions {
    override fun onOpenFolder(folder: FolderModel) = Unit
    override fun onNavigateUp() = Unit
    override fun onNavigateToBreadcrumb(index: Int) = Unit
    override fun onAddClick() = Unit
    override fun onDismissCreateDialog() = Unit
    override fun onNameChange(value: String) = Unit
    override fun onCreateSubmit() = Unit
    override fun onRenameClick(folder: FolderModel) = Unit
    override fun onRenameChange(value: String) = Unit
    override fun onRenameSubmit() = Unit
    override fun onDismissRename() = Unit
    override fun onDeleteClick(folder: FolderModel) = Unit
    override fun onConfirmDelete() = Unit
    override fun onDismissDelete() = Unit
}
