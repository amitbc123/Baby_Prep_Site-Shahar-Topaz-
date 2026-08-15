package com.oryareach.core.domain.cycle

import com.oryareach.core.model.MenstrualCycle
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test

private fun cycle(start: LocalDate, end: LocalDate? = null) =
    MenstrualCycle(id = start.toString(), startDate = start, endDate = end)

class CycleStatisticsTest {

    @Test
    fun `empty history has zero count and null figures`() {
        val stats = calculateCycleStatistics(emptyList())
        stats.cycleCount shouldBe 0
        stats.averageCycleLengthDays shouldBe null
        stats.shortestCycleLengthDays shouldBe null
        stats.longestCycleLengthDays shouldBe null
        stats.averagePeriodLengthDays shouldBe null
    }

    @Test
    fun `a single cycle has no cycle-length figures but does have a period length`() {
        val stats = calculateCycleStatistics(
            listOf(cycle(LocalDate(2026, 7, 1), LocalDate(2026, 7, 5))),
        )
        stats.cycleCount shouldBe 1
        stats.averageCycleLengthDays shouldBe null
        stats.averagePeriodLengthDays shouldBe 5
    }

    @Test
    fun `shortest and longest track the min and max gap`() {
        val stats = calculateCycleStatistics(
            listOf(
                cycle(LocalDate(2026, 5, 1)),
                cycle(LocalDate(2026, 5, 29)), // 28-day gap
                cycle(LocalDate(2026, 6, 28)), // 30-day gap
            ),
        )
        stats.shortestCycleLengthDays shouldBe 28
        stats.longestCycleLengthDays shouldBe 30
        stats.averageCycleLengthDays shouldBe 29
    }

    @Test
    fun `an ongoing period is excluded from the period-length average`() {
        val stats = calculateCycleStatistics(
            listOf(
                cycle(LocalDate(2026, 7, 1), LocalDate(2026, 7, 5)),
                cycle(LocalDate(2026, 8, 1)), // still ongoing, no end date
            ),
        )
        stats.averagePeriodLengthDays shouldBe 5
    }
}
