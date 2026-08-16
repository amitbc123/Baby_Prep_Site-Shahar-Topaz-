package com.oryareach.core.domain.task

import com.oryareach.core.model.Recurrence
import com.oryareach.core.model.RecurrenceFrequency
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test

class RecurrenceAdvanceTest {

    @Test
    fun `daily advances by interval days`() {
        nextDueDate(LocalDate(2026, 8, 10), Recurrence(RecurrenceFrequency.DAILY)) shouldBe LocalDate(2026, 8, 11)
        nextDueDate(LocalDate(2026, 8, 10), Recurrence(RecurrenceFrequency.DAILY, interval = 3)) shouldBe LocalDate(2026, 8, 13)
    }

    @Test
    fun `weekly advances by interval weeks, same day of week`() {
        val tuesday = LocalDate(2026, 8, 11)
        nextDueDate(tuesday, Recurrence(RecurrenceFrequency.WEEKLY)) shouldBe LocalDate(2026, 8, 18)
        nextDueDate(tuesday, Recurrence(RecurrenceFrequency.WEEKLY, interval = 2)) shouldBe LocalDate(2026, 8, 25)
    }

    @Test
    fun `monthly preserves day of month when the target month has it`() {
        nextDueDate(LocalDate(2026, 8, 15), Recurrence(RecurrenceFrequency.MONTHLY)) shouldBe LocalDate(2026, 9, 15)
    }

    @Test
    fun `monthly clamps to the last day when the target month is shorter`() {
        nextDueDate(LocalDate(2026, 1, 31), Recurrence(RecurrenceFrequency.MONTHLY)) shouldBe LocalDate(2026, 2, 28)
    }
}
