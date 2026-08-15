package com.oryareach.feature.cycle

import androidx.compose.runtime.Immutable
import com.oryareach.core.model.MenstrualCycle

@Immutable
data class CycleUiState(
    // Persisted snapshot: the live data from Room, already decrypted.
    val ongoing: MenstrualCycle? = null,
    val history: List<MenstrualCycle> = emptyList(),

    // Transient UI-only.
    val busy: Boolean = false,
) {
    val isPeriodOngoing: Boolean get() = ongoing != null
}
