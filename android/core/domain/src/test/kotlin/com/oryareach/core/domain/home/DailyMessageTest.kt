package com.oryareach.core.domain.home

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test

class DailyMessageTest {

    @Test
    fun `first day of the year picks index 0`() {
        dailyMessageIndex(LocalDate(2026, 1, 1), messageCount = 6) shouldBe 0
    }

    @Test
    fun `wraps around once the day of year exceeds the message count`() {
        dailyMessageIndex(LocalDate(2026, 1, 7), messageCount = 6) shouldBe 0
    }

    @Test
    fun `advances by one each day within a cycle`() {
        dailyMessageIndex(LocalDate(2026, 1, 3), messageCount = 6) shouldBe 2
    }
}
