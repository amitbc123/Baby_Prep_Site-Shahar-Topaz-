package com.oryareach.core.domain.pregnancy

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Test

class PregnancyProgressTest {

    @Test
    fun `counts whole days between two dates`() {
        daysUntil(LocalDate(2026, 9, 1), LocalDate(2026, 8, 25)) shouldBe 7
    }

    @Test
    fun `is negative once the due date has passed`() {
        daysUntil(LocalDate(2026, 1, 1), LocalDate(2026, 1, 5)) shouldBe -4
    }

    @Test
    fun `is zero on the due date itself`() {
        daysUntil(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1)) shouldBe 0
    }

    @Test
    fun `reports week 1 day 1 far before the due date`() {
        val progress = getPregnancyProgress(LocalDate(2026, 12, 31), LocalDate(2026, 1, 1))
        progress.week shouldBe 1
        progress.dayOfWeek shouldBe 1
        progress.hasArrived shouldBe false
    }

    @Test
    fun `reports week 40 and hasArrived on the due date`() {
        val progress = getPregnancyProgress(LocalDate(2026, 6, 1), LocalDate(2026, 6, 1))
        progress.week shouldBe 40
        progress.hasArrived shouldBe true
        progress.moonFraction shouldBe 1f
    }

    @Test
    fun `advances day-of-week within a pregnancy week`() {
        val from = LocalDate(2026, 1, 1)
        val due = from.plus(280 - 10, kotlinx.datetime.DateTimeUnit.DAY) // 10 days elapsed -> week 2, day 4
        val progress = getPregnancyProgress(due, from)
        progress.week shouldBe 2
        progress.dayOfWeek shouldBe 4
    }

    @Test
    fun `clamps moonFraction and week at term even past the due date`() {
        val progress = getPregnancyProgress(LocalDate(2026, 1, 1), LocalDate(2026, 2, 1))
        progress.week shouldBe 40
        progress.moonFraction shouldBe 1f
    }

    @Test
    fun `isPastDate treats the same day as not past`() {
        isPastDate(LocalDate(2026, 3, 10), LocalDate(2026, 3, 10)) shouldBe false
    }

    @Test
    fun `isPastDate treats an earlier date as past`() {
        isPastDate(LocalDate(2026, 3, 10), LocalDate(2026, 3, 11)) shouldBe true
    }
}
