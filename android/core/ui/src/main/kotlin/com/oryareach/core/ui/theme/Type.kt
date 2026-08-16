package com.oryareach.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oryareach.core.ui.R

/**
 * Assistant for headings, Heebo for body — the same pairing as the web app, and both cover
 * Hebrew and Latin so a bilingual UI does not fall back to a different face per language.
 *
 * Bundled as variable fonts (one file per family, weights synthesised from the wght axis)
 * rather than a static weight per file, which keeps the APK smaller.
 */
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Assistant = FontFamily(
    variableFont(R.font.assistant_variable, FontWeight.Normal),
    variableFont(R.font.assistant_variable, FontWeight.Medium),
    variableFont(R.font.assistant_variable, FontWeight.SemiBold),
    variableFont(R.font.assistant_variable, FontWeight.Bold),
)

private val Heebo = FontFamily(
    variableFont(R.font.heebo_variable, FontWeight.Light),
    variableFont(R.font.heebo_variable, FontWeight.Normal),
    variableFont(R.font.heebo_variable, FontWeight.Medium),
    variableFont(R.font.heebo_variable, FontWeight.SemiBold),
    variableFont(R.font.heebo_variable, FontWeight.Bold),
)

private val default = Typography()

/** Headings use Assistant at 600, matching the web app's `.font-heading` rule. */
val OrYareachTypography = Typography(
    displayLarge = default.displayLarge.heading(),
    displayMedium = default.displayMedium.heading(),
    displaySmall = default.displaySmall.heading(),
    headlineLarge = default.headlineLarge.heading(),
    // The page-title style every screen uses (added Phase 12) — tightened letter spacing and
    // bumped size/weight slightly past the stock M3 default so it reads as an intentional
    // typographic choice rather than an unmodified Material default sitting above the content.
    headlineMedium = default.headlineMedium.heading().copy(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = default.headlineSmall.heading(),
    titleLarge = default.titleLarge.heading(),
    titleMedium = default.titleMedium.heading(),
    titleSmall = default.titleSmall.heading(),
    bodyLarge = default.bodyLarge.body(),
    bodyMedium = default.bodyMedium.body(),
    bodySmall = default.bodySmall.body(),
    labelLarge = default.labelLarge.body(),
    labelMedium = default.labelMedium.body(),
    labelSmall = default.labelSmall.body(),
)

private fun TextStyle.heading() = copy(fontFamily = Assistant, fontWeight = FontWeight.SemiBold)

private fun TextStyle.body() = copy(fontFamily = Heebo)

/** Used by the moon countdown's day counter, which needs a larger figure than M3 provides. */
val CountdownNumberStyle = TextStyle(
    fontFamily = Assistant,
    fontWeight = FontWeight.Bold,
    fontSize = 56.sp,
)
