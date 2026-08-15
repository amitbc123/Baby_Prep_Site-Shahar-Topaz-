package com.oryareach.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The two palette entries that have no Material 3 slot. Exposed through a composition local
 * rather than as loose constants so they follow the light/dark switch like every other color.
 */
@Immutable
data class ExtendedColors(
    val moss: Color,
    val onMoss: Color,
    val blush: Color,
    val onBlush: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        moss = Palette.Light.moss,
        onMoss = Palette.Light.mossForeground,
        blush = Palette.Light.blush,
        onBlush = Palette.Light.blushForeground,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Palette.Light.primary,
    onPrimary = Palette.Light.primaryForeground,
    secondary = Palette.Light.secondary,
    onSecondary = Palette.Light.secondaryForeground,
    tertiary = Palette.Light.blush,
    onTertiary = Palette.Light.blushForeground,
    background = Palette.Light.background,
    onBackground = Palette.Light.foreground,
    surface = Palette.Light.card,
    onSurface = Palette.Light.cardForeground,
    surfaceVariant = Palette.Light.muted,
    onSurfaceVariant = Palette.Light.mutedForeground,
    surfaceContainer = Palette.Light.accent,
    error = Palette.Light.destructive,
    onError = Palette.Light.primaryForeground,
    outline = Palette.Light.border,
    outlineVariant = Palette.Light.border,
)

private val DarkColorScheme = darkColorScheme(
    primary = Palette.Dark.primary,
    onPrimary = Palette.Dark.primaryForeground,
    secondary = Palette.Dark.secondary,
    onSecondary = Palette.Dark.secondaryForeground,
    tertiary = Palette.Dark.blush,
    onTertiary = Palette.Dark.blushForeground,
    background = Palette.Dark.background,
    onBackground = Palette.Dark.foreground,
    surface = Palette.Dark.card,
    onSurface = Palette.Dark.cardForeground,
    surfaceVariant = Palette.Dark.muted,
    onSurfaceVariant = Palette.Dark.mutedForeground,
    surfaceContainer = Palette.Dark.accent,
    error = Palette.Dark.destructive,
    onError = Palette.Dark.primaryForeground,
    outline = Palette.Dark.border,
    outlineVariant = Palette.Dark.border,
)

/**
 * Dynamic color is deliberately not used: the palette is the product's identity, carried
 * over from the web app, and letting the wallpaper recolor it would lose that.
 */
@Composable
fun OrYareachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) {
        ExtendedColors(
            moss = Palette.Dark.moss,
            onMoss = Palette.Dark.mossForeground,
            blush = Palette.Dark.blush,
            onBlush = Palette.Dark.blushForeground,
        )
    } else {
        ExtendedColors(
            moss = Palette.Light.moss,
            onMoss = Palette.Light.mossForeground,
            blush = Palette.Light.blush,
            onBlush = Palette.Light.blushForeground,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OrYareachTypography,
            shapes = OrYareachShapes,
            content = content,
        )
    }
}

object OrYareachTheme {
    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

    /** The always-dark palette used by the moon countdown, independent of the app theme. */
    val night = NightPalette
}
