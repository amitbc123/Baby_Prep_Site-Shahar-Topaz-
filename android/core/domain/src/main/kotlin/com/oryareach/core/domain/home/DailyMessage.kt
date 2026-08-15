package com.oryareach.core.domain.home

import kotlinx.datetime.LocalDate

/**
 * Which of [messageCount] daily messages to show today. The messages themselves are UI copy
 * (a string-array resource, so they can be translated) — this only picks the stable index.
 */
fun dailyMessageIndex(date: LocalDate, messageCount: Int): Int = (date.dayOfYear - 1) % messageCount
