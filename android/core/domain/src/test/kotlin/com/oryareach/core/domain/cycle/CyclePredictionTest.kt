package com.oryareach.core.domain.cycle

import com.oryareach.core.model.MenstrualCycle
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test

private fun cycle(start: LocalDate, end: LocalDate? = null) =
    MenstrualCycle(id = start.toString(), startDate = start, endDate = end)

class CyclePredictionTest {

    @Test
    fun `no history is insufficient`() {
        predictNextCycle(emptyList()).hasSufficientHistory shouldBe false
    }

    @Test
    fun `a single logged period is insufficient`() {
        val result = predictNextCycle(listOf(cycle(LocalDate(2026, 7, 1))))
        result.hasSufficientHistory shouldBe false
        result.nextPeriodStart shouldBe null
    }

    @Test
    fun `two periods 28 days apart predict a 28-day next cycle`() {
        val result = predictNextCycle(
            listOf(
                cycle(LocalDate(2026, 7, 1)),
                cycle(LocalDate(2026, 7, 29)),
            ),
        )
        result.hasSufficientHistory shouldBe true
        result.averageCycleLengthDays shouldBe 28
        result.nextPeriodStart shouldBe LocalDate(2026, 8, 26)
    }

    @Test
    fun `ovulation and fertile window sit before the predicted next period`() {
        val result = predictNextCycle(
            listOf(
                cycle(LocalDate(2026, 7, 1)),
                cycle(LocalDate(2026, 7, 29)),
            ),
        )
        result.ovulationDate shouldBe LocalDate(2026, 8, 12)
        result.fertileWindowStart shouldBe LocalDate(2026, 8, 7)
        result.fertileWindowEnd shouldBe LocalDate(2026, 8, 13)
    }

    @Test
    fun `unsorted history is sorted before computing gaps`() {
        val result = predictNextCycle(
            listOf(
                cycle(LocalDate(2026, 7, 29)),
                cycle(LocalDate(2026, 7, 1)),
            ),
        )
        result.averageCycleLengthDays shouldBe 28
        result.nextPeriodStart shouldBe LocalDate(2026, 8, 26)
    }

    @Test
    fun `three periods average their two gaps`() {
        val result = predictNextCycle(
            listOf(
                cycle(LocalDate(2026, 5, 1)),
                cycle(LocalDate(2026, 5, 29)), // 28-day gap
                cycle(LocalDate(2026, 6, 28)), // 30-day gap
            ),
        )
        result.averageCycleLengthDays shouldBe 29
        result.nextPeriodStart shouldBe LocalDate(2026, 7, 27)
    }
}
