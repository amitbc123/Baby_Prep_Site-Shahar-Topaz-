package com.oryareach.core.domain.pregnancy

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val FULL_TERM_WEEKS = 40
private const val TOTAL_DAYS = FULL_TERM_WEEKS * 7

/** Naegele's rule: due date = first day of the last menstrual period + 280 days (40 weeks).
 * The standard obstetric estimate, and more accurate as a starting point than a due date
 * entered directly — a due date someone remembers or was told is one hop removed from the
 * actual measurement (when the last period started), which introduces room for drift that
 * computing it fresh from the LMP avoids. */
private const val LMP_TO_DUE_DATE_DAYS = TOTAL_DAYS

fun dueDateFromLastPeriod(lastPeriodStart: LocalDate): LocalDate =
    lastPeriodStart.plus(LMP_TO_DUE_DATE_DAYS, DateTimeUnit.DAY)

/** Inverse of [dueDateFromLastPeriod] — exact, since the forward direction is a fixed
 * day-count offset with no rounding. Used to re-derive the last-period date for display when
 * re-opening the editor, since only [com.oryareach.core.model.AppSettings.dueDate] is stored. */
fun lastPeriodFromDueDate(dueDate: LocalDate): LocalDate =
    dueDate.minus(LMP_TO_DUE_DATE_DAYS, DateTimeUnit.DAY)

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
