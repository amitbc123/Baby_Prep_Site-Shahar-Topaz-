package com.oryareach.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class FlowLevel {
    SPOTTING,
    LIGHT,
    MEDIUM,
    HEAVY,
}

enum class PainLevel {
    NONE,
    MILD,
    MODERATE,
    SEVERE,
}

enum class Symptom {
    CRAMPS,
    HEADACHE,
    BLOATING,
    FATIGUE,
    NAUSEA,
    BACKACHE,
    TENDER_BREASTS,
    ACNE,
}

enum class Mood {
    HAPPY,
    CALM,
    SAD,
    IRRITABLE,
    ANXIOUS,
    ENERGETIC,
    TIRED,
}

/**
 * A day's worth of logged detail — flow, symptoms, mood, pain, a free-text note. Separate
 * from [MenstrualCycle] (which only records a period's start/end) because most logged days
 * are not period days at all: symptoms and mood are tracked across the whole cycle, not just
 * during bleeding. At most one entry per calendar date; a second log for the same date
 * replaces the first rather than creating a duplicate.
 */
@Serializable
data class CycleEntry(
    val id: String,
    val date: LocalDate,
    val flow: FlowLevel? = null,
    val symptoms: List<Symptom> = emptyList(),
    val mood: List<Mood> = emptyList(),
    val pain: PainLevel? = null,
    val note: String? = null,
)
