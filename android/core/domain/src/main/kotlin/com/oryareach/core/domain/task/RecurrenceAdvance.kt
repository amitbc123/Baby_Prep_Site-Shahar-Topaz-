package com.oryareach.core.domain.task

import com.oryareach.core.model.Recurrence
import com.oryareach.core.model.RecurrenceFrequency
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Advances [from] by one occurrence of [recurrence] — "every 2 weeks" from a Tuesday lands
 * on the Tuesday two weeks later, not "14 days from today"; monthly preserves the day of
 * month where the target month has it, and clamps to the last day where it doesn't (Jan 31
 * recurring monthly lands on Feb 28/29, not March 3). */
fun nextDueDate(from: LocalDate, recurrence: Recurrence): LocalDate = when (recurrence.frequency) {
    RecurrenceFrequency.DAILY -> from.plus(recurrence.interval, DateTimeUnit.DAY)
    RecurrenceFrequency.WEEKLY -> from.plus(recurrence.interval * 7, DateTimeUnit.DAY)
    RecurrenceFrequency.MONTHLY -> from.plus(recurrence.interval, DateTimeUnit.MONTH)
}
