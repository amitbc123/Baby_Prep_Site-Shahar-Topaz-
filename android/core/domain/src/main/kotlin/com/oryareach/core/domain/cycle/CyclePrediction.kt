package com.oryareach.core.domain.cycle

import com.oryareach.core.model.MenstrualCycle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/** Cycle length needs at least this many recorded starts (one gap) to say anything. */
private const val MIN_STARTS_FOR_PREDICTION = 2

/** Textbook luteal-phase length, used only to place an estimated ovulation date relative to
 * the next predicted period — never presented as a measured fact. */
private const val LUTEAL_PHASE_DAYS = 14
private const val FERTILE_WINDOW_DAYS_BEFORE_OVULATION = 5

data class CyclePrediction(
    val hasSufficientHistory: Boolean,
    val averageCycleLengthDays: Int? = null,
    val nextPeriodStart: LocalDate? = null,
    val ovulationDate: LocalDate? = null,
    val fertileWindowStart: LocalDate? = null,
    val fertileWindowEnd: LocalDate? = null,
)

/**
 * Cycle length is the gap between consecutive period *start* dates, not a single period's
 * start-to-end length. With fewer than [MIN_STARTS_FOR_PREDICTION] recorded starts there is no
 * gap to measure, so this returns [CyclePrediction.hasSufficientHistory] = false rather than
 * guessing with a textbook default — presenting a guess as a fact is exactly what a prediction
 * feature must not do.
 */
fun predictNextCycle(history: List<MenstrualCycle>): CyclePrediction {
    val startDates = history.map { it.startDate }.sorted()
    if (startDates.size < MIN_STARTS_FOR_PREDICTION) return CyclePrediction(hasSufficientHistory = false)

    val gaps = startDates.zipWithNext { a, b -> (b.toEpochDays() - a.toEpochDays()) }
    val averageLength = gaps.average().roundToInt()

    val nextStart = startDates.last().plusDays(averageLength)
    val ovulation = nextStart.plusDays(-LUTEAL_PHASE_DAYS)

    return CyclePrediction(
        hasSufficientHistory = true,
        averageCycleLengthDays = averageLength,
        nextPeriodStart = nextStart,
        ovulationDate = ovulation,
        fertileWindowStart = ovulation.plusDays(-FERTILE_WINDOW_DAYS_BEFORE_OVULATION),
        fertileWindowEnd = ovulation.plusDays(1),
    )
}

private fun LocalDate.plusDays(days: Int): LocalDate = this.plus(days, DateTimeUnit.DAY)
