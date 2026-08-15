package com.oryareach.feature.cycle

import androidx.annotation.StringRes
import com.oryareach.core.model.FlowLevel
import com.oryareach.core.model.Mood
import com.oryareach.core.model.PainLevel
import com.oryareach.core.model.Symptom

@StringRes
internal fun FlowLevel.labelRes(): Int = when (this) {
    FlowLevel.SPOTTING -> R.string.cycle_flow_spotting
    FlowLevel.LIGHT -> R.string.cycle_flow_light
    FlowLevel.MEDIUM -> R.string.cycle_flow_medium
    FlowLevel.HEAVY -> R.string.cycle_flow_heavy
}

@StringRes
internal fun PainLevel.labelRes(): Int = when (this) {
    PainLevel.NONE -> R.string.cycle_pain_none
    PainLevel.MILD -> R.string.cycle_pain_mild
    PainLevel.MODERATE -> R.string.cycle_pain_moderate
    PainLevel.SEVERE -> R.string.cycle_pain_severe
}

@StringRes
internal fun Symptom.labelRes(): Int = when (this) {
    Symptom.CRAMPS -> R.string.cycle_symptom_cramps
    Symptom.HEADACHE -> R.string.cycle_symptom_headache
    Symptom.BLOATING -> R.string.cycle_symptom_bloating
    Symptom.FATIGUE -> R.string.cycle_symptom_fatigue
    Symptom.NAUSEA -> R.string.cycle_symptom_nausea
    Symptom.BACKACHE -> R.string.cycle_symptom_backache
    Symptom.TENDER_BREASTS -> R.string.cycle_symptom_tender_breasts
    Symptom.ACNE -> R.string.cycle_symptom_acne
}

@StringRes
internal fun Mood.labelRes(): Int = when (this) {
    Mood.HAPPY -> R.string.cycle_mood_happy
    Mood.CALM -> R.string.cycle_mood_calm
    Mood.SAD -> R.string.cycle_mood_sad
    Mood.IRRITABLE -> R.string.cycle_mood_irritable
    Mood.ANXIOUS -> R.string.cycle_mood_anxious
    Mood.ENERGETIC -> R.string.cycle_mood_energetic
    Mood.TIRED -> R.string.cycle_mood_tired
}
