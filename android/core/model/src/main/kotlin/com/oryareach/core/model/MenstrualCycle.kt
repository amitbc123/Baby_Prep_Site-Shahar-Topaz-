package com.oryareach.core.model

import kotlinx.datetime.LocalDate

/**
 * One recorded menstrual period.
 *
 * Only user-entered facts are stored. Cycle length, predicted dates, fertile window and
 * ovulation estimates are all derived from the recorded history at read time, never
 * persisted — a stored prediction goes stale the moment a new period is logged, and
 * presenting a stale estimate as a fact is exactly what the product must not do.
 */
data class MenstrualCycle(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val note: String? = null,
) {
    init {
        require(endDate == null || endDate >= startDate) {
            "a period cannot end before it starts"
        }
    }

    /** Null while the period is still in progress. */
    val periodLengthDays: Int?
        get() = endDate?.let { (it.toEpochDays() - startDate.toEpochDays() + 1).toInt() }

    val isOngoing: Boolean get() = endDate == null
}
