package com.oryareach.core.domain.pregnancy

import kotlinx.datetime.LocalDate

private const val FULL_TERM_WEEKS = 40
private const val TOTAL_DAYS = FULL_TERM_WEEKS * 7

fun daysUntil(dueDate: LocalDate, from: LocalDate): Int =
    (dueDate.toEpochDays() - from.toEpochDays()).toInt()

data class PregnancyProgress(
    val daysLeft: Int,
    val week: Int,
    /** 1..7, the day within the current pregnancy week. */
    val dayOfWeek: Int,
    val hasArrived: Boolean,
    /** 0 (new crescent) .. 1 (full moon at due date). */
    val moonFraction: Float,
)

fun getPregnancyProgress(dueDate: LocalDate, from: LocalDate): PregnancyProgress {
    val daysLeft = daysUntil(dueDate, from)
    val daysElapsed = TOTAL_DAYS - daysLeft
    val clampedElapsed = daysElapsed.coerceIn(0, TOTAL_DAYS - 1)
    val week = (daysElapsed / 7 + 1).coerceIn(1, FULL_TERM_WEEKS)
    val dayOfWeek = clampedElapsed % 7 + 1
    val moonFraction = (daysElapsed.toFloat() / TOTAL_DAYS).coerceIn(0f, 1f)
    return PregnancyProgress(
        daysLeft = daysLeft,
        week = week,
        dayOfWeek = dayOfWeek,
        hasArrived = daysLeft <= 0,
        moonFraction = moonFraction,
    )
}

fun isPastDate(date: LocalDate, from: LocalDate): Boolean = date < from
