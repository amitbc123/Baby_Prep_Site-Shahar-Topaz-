package com.oryareach.core.domain.cycle

import com.oryareach.core.model.MenstrualCycle
import kotlin.math.roundToInt

data class CycleStatistics(
    val cycleCount: Int,
    val averageCycleLengthDays: Int? = null,
    val shortestCycleLengthDays: Int? = null,
    val longestCycleLengthDays: Int? = null,
    val averagePeriodLengthDays: Int? = null,
)

/** Cycle-length figures come from gaps between consecutive start dates (needs 2+ cycles);
 * period-length figures come from completed periods only (an ongoing one has no end yet). */
fun calculateCycleStatistics(history: List<MenstrualCycle>): CycleStatistics {
    val startDates = history.map { it.startDate }.sorted()
    val gaps = startDates.zipWithNext { a, b -> (b.toEpochDays() - a.toEpochDays()).toInt() }
    val periodLengths = history.mapNotNull { it.periodLengthDays }

    return CycleStatistics(
        cycleCount = history.size,
        averageCycleLengthDays = gaps.averageOrNull(),
        shortestCycleLengthDays = gaps.minOrNull(),
        longestCycleLengthDays = gaps.maxOrNull(),
        averagePeriodLengthDays = periodLengths.averageOrNull(),
    )
}

private fun List<Int>.averageOrNull(): Int? = if (isEmpty()) null else (sum().toDouble() / size).roundToInt()
