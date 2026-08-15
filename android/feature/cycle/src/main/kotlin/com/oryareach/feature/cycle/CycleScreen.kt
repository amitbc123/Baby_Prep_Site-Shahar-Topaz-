package com.oryareach.feature.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.model.MenstrualCycle
import com.oryareach.core.ui.text.asLtrIsolate
import com.oryareach.core.ui.theme.OrYareachTheme
import kotlinx.datetime.LocalDate

@Composable
fun CycleScreen(
    uiState: CycleUiState,
    actions: CycleActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.cycle_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            item { OngoingCard(uiState = uiState, actions = actions) }

            item {
                Text(
                    text = stringResource(R.string.cycle_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (uiState.history.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.cycle_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.history, key = MenstrualCycle::id) { cycle ->
                    HistoryRow(cycle = cycle, onDelete = { actions.onDelete(cycle.id) })
                }
            }
        }
    }
}

@Composable
private fun OngoingCard(uiState: CycleUiState, actions: CycleActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val ongoing = uiState.ongoing
            Text(
                text = if (ongoing != null) {
                    stringResource(R.string.cycle_ongoing_started, ongoing.startDate.toString().asLtrIsolate())
                } else {
                    stringResource(R.string.cycle_no_ongoing)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (ongoing == null) {
                Button(
                    onClick = actions::onStartPeriod,
                    enabled = !uiState.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cycle_start))
                }
            } else {
                OutlinedButton(
                    onClick = actions::onEndPeriod,
                    enabled = !uiState.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cycle_end))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(cycle: MenstrualCycle, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val text = cycle.endDate?.let { end ->
                stringResource(
                    R.string.cycle_history_row_range,
                    cycle.startDate.toString().asLtrIsolate(),
                    end.toString().asLtrIsolate(),
                    cycle.periodLengthDays ?: 0,
                )
            } ?: stringResource(R.string.cycle_history_row_ongoing, cycle.startDate.toString().asLtrIsolate())

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cycle_delete))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CyclePreview() {
    OrYareachTheme {
        CycleScreen(
            uiState = CycleUiState(
                ongoing = MenstrualCycle(id = "1", startDate = LocalDate(2026, 8, 10)),
                history = listOf(
                    MenstrualCycle(id = "1", startDate = LocalDate(2026, 8, 10)),
                    MenstrualCycle(
                        id = "2",
                        startDate = LocalDate(2026, 7, 12),
                        endDate = LocalDate(2026, 7, 17),
                    ),
                ),
            ),
            actions = NoopCycleActions,
        )
    }
}

private object NoopCycleActions : CycleActions {
    override fun onStartPeriod() = Unit
    override fun onEndPeriod() = Unit
    override fun onDelete(id: String) = Unit
}
