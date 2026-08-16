package com.oryareach.feature.conflicts

import androidx.compose.runtime.Immutable
import com.oryareach.core.database.repository.Conflict

@Immutable
data class ConflictsUiState(
    val conflicts: List<Conflict> = emptyList(),
    val resolvingId: String? = null,
) {
    val hasConflicts: Boolean get() = conflicts.isNotEmpty()
}
