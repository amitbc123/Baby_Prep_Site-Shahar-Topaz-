package com.oryareach.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.Task
import com.oryareach.core.model.TaskCategory
import com.oryareach.core.ui.theme.OrYareachTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    uiState: TasksUiState,
    actions: TasksActions,
    modifier: Modifier = Modifier,
) {
    val hospitalBagTitles = androidx.compose.ui.res.stringArrayResource(R.array.hospital_bag_preset).toList()

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        floatingActionButton = {
            FloatingActionButton(onClick = actions::onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tasks_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.material3.TextButton(
                onClick = { actions.onSeedHospitalBag(hospitalBagTitles) },
                enabled = !uiState.seedingHospitalBag,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tasks_seed_hospital_bag))
            }

            if (uiState.tasks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.tasks_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.tasks, key = Task::id) { task ->
                        TaskRow(task = task, actions = actions)
                    }
                }
            }
        }
    }

    if (uiState.sheetVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = actions::onDismissSheet, sheetState = sheetState) {
            TaskForm(uiState = uiState, actions = actions)
        }
    }
}

@Composable
private fun TaskRow(task: Task, actions: TasksActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.done, onCheckedChange = { actions.onToggleDone(task.id) })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .clickable { actions.onEditClick(task) },
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(task.category.labelRes()) +
                        " · " + stringResource(task.priority.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { actions.onDelete(task.id) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.tasks_delete),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskForm(uiState: TasksUiState, actions: TasksActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (uiState.isEditing) R.string.tasks_edit_title else R.string.tasks_add_title),
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = uiState.formTitle,
            onValueChange = actions::onTitleChange,
            label = { Text(stringResource(R.string.tasks_field_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        CategoryDropdown(value = uiState.formCategory, onChange = actions::onCategoryChange)

        Text(
            text = stringResource(R.string.tasks_field_priority),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Priority.entries.forEachIndexed { index, priority ->
                SegmentedButton(
                    selected = uiState.formPriority == priority,
                    onClick = { actions.onPriorityChange(priority) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = Priority.entries.size,
                    ),
                ) {
                    Text(stringResource(priority.labelRes()))
                }
            }
        }

        Text(
            text = stringResource(R.string.tasks_field_assignee),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options: List<Assignee?> = listOf(null, Assignee.PARTNER_ONE, Assignee.PARTNER_TWO, Assignee.BOTH)
            options.forEach { option ->
                FilterChip(
                    selected = uiState.formAssignee == option,
                    onClick = { actions.onAssigneeChange(option) },
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }

        OutlinedTextField(
            value = uiState.formNote,
            onValueChange = actions::onNoteChange,
            label = { Text(stringResource(R.string.tasks_field_note)) },
            modifier = Modifier.fillMaxWidth(),
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(4.dp))

        androidx.compose.material3.Button(
            onClick = actions::onSubmit,
            enabled = uiState.canSubmitForm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tasks_save))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryDropdown(value: TaskCategory, onChange: (TaskCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tasks_field_category) + ": " + stringResource(value.labelRes()))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes())) },
                    onClick = {
                        onChange(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksPreview() {
    OrYareachTheme {
        TasksScreen(
            uiState = TasksUiState(
                tasks = listOf(
                    Task(id = "1", title = "Pack hospital bag", category = TaskCategory.HOSPITAL_BAG),
                    Task(id = "2", title = "Book pediatrician", category = TaskCategory.MEDICAL, done = true),
                ),
            ),
            actions = NoopTasksActions,
        )
    }
}

private object NoopTasksActions : TasksActions {
    override fun onAddClick() = Unit
    override fun onEditClick(task: Task) = Unit
    override fun onDismissSheet() = Unit
    override fun onTitleChange(value: String) = Unit
    override fun onCategoryChange(value: TaskCategory) = Unit
    override fun onPriorityChange(value: Priority) = Unit
    override fun onAssigneeChange(value: Assignee?) = Unit
    override fun onNoteChange(value: String) = Unit
    override fun onSubmit() = Unit
    override fun onToggleDone(id: String) = Unit
    override fun onDelete(id: String) = Unit
    override fun onSeedHospitalBag(titles: List<String>) = Unit
}
