package com.oryareach.feature.folders

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.model.Document
import com.oryareach.core.model.Folder as FolderModel
import com.oryareach.core.scanner.rememberDocumentScanner
import com.oryareach.core.ui.theme.OrYareachTheme
import java.io.File

@Composable
fun FoldersScreen(
    uiState: FoldersUiState,
    actions: FoldersActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var fabExpanded by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment.orEmpty()
        actions.onDocumentPicked(name, mimeType, bytes)
    }
    val startScan = rememberDocumentScanner { scanned ->
        actions.onDocumentPicked(scanned.name, scanned.mimeType, scanned.bytes)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (fabExpanded) {
                    val scanLabel = stringResource(R.string.folders_scan)
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; startScan() },
                        icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                        text = { Text(scanLabel) },
                        modifier = Modifier.semantics { contentDescription = scanLabel },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val importLabel = stringResource(R.string.folders_import)
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; importLauncher.launch(arrayOf("*/*")) },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                        text = { Text(importLabel) },
                        modifier = Modifier.semantics { contentDescription = importLabel },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val addLabel = stringResource(R.string.folders_add)
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; actions.onAddClick() },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        text = { Text(addLabel) },
                        modifier = Modifier.semantics { contentDescription = addLabel },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.folders_add))
                }
            }
        },
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = actions::onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Breadcrumb(uiState = uiState, actions = actions)

            if (uiState.importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.children.isEmpty() && uiState.documents.isEmpty()) {
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
                    items(uiState.documents, key = Document::id) { document ->
                        DocumentRow(document = document, actions = actions)
                    }
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

    uiState.deleteConfirmDocument?.let { document ->
        AlertDialog(
            onDismissRequest = actions::onDismissDeleteDocument,
            title = { Text(stringResource(R.string.folders_delete_document_title)) },
            text = { Text(stringResource(R.string.folders_delete_document_body, document.name)) },
            confirmButton = {
                TextButton(onClick = actions::onConfirmDeleteDocument) { Text(stringResource(R.string.folders_delete)) }
            },
            dismissButton = {
                TextButton(onClick = actions::onDismissDeleteDocument) { Text(stringResource(R.string.folders_cancel)) }
            },
        )
    }

    uiState.previewDocument?.let { document ->
        AlertDialog(
            onDismissRequest = actions::onDismissPreview,
            title = { Text(document.name) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when {
                        uiState.previewLoading -> CircularProgressIndicator()
                        else -> when (val content = uiState.previewContent) {
                            is DocumentPreview.Text -> Text(content.content)
                            is DocumentPreview.Image -> {
                                val bitmap = remember(content.bytes) {
                                    android.graphics.BitmapFactory.decodeByteArray(content.bytes, 0, content.bytes.size)
                                }
                                bitmap?.let {
                                    Image(bitmap = it.asImageBitmap(), contentDescription = document.name, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            is DocumentPreview.Pdf -> PdfPreview(bytes = content.pageBytes)
                            DocumentPreview.Unsupported -> Text(stringResource(R.string.folders_preview_unsupported))
                            DocumentPreview.Failed, null -> Text(stringResource(R.string.folders_preview_failed))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = actions::onDismissPreview) { Text(stringResource(R.string.folders_preview_close)) }
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
private fun DocumentRow(document: Document, actions: FoldersActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DocumentThumbnail(document = document)
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 12.dp).clickable { actions.onPreviewDocument(document) },
            )
            IconButton(onClick = { actions.onDeleteDocumentClick(document) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.folders_delete))
            }
        }
    }
}

/** Shows the small on-device-generated preview (see `Document.thumbnailBase64`'s doc comment)
 * for an image document, decoded once per composition and cached by [remember] so scrolling
 * the list doesn't redecode the same Base64 string every frame. Falls back to the generic file
 * icon for non-image documents, or an image whose preview failed to decode. */
@Composable
private fun DocumentThumbnail(document: Document) {
    val bitmap = remember(document.thumbnailBase64) {
        document.thumbnailBase64?.let { base64 ->
            runCatching {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
    }
}

/** Renders page 1 of the PDF to a bitmap via [PdfRenderer], which needs a real file — the
 * bytes are written to a throwaway cache file for the duration of the render. */
@Composable
private fun PdfPreview(bytes: ByteArray) {
    val context = LocalContext.current
    var bitmap by remember(bytes) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bytes) {
        val file = File(context.cacheDir, "preview-${System.nanoTime()}.pdf")
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val rendered = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap = rendered
                    }
                }
            }
        } finally {
            file.delete()
        }
    }

    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
    } ?: CircularProgressIndicator(modifier = Modifier.size(32.dp))
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
    override fun onDocumentPicked(name: String, mimeType: String, bytes: ByteArray) = Unit
    override fun onDeleteDocumentClick(document: Document) = Unit
    override fun onConfirmDeleteDocument() = Unit
    override fun onDismissDeleteDocument() = Unit
    override fun onPreviewDocument(document: Document) = Unit
    override fun onDismissPreview() = Unit
    override fun onRefresh() = Unit
}
