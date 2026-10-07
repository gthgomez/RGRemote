package com.rgremote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** User-selectable appearance mode; SYSTEM follows the OS setting. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

private val RgRemoteDarkColors = darkColorScheme(
    primary = RgRokuPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF321D68),
    onPrimaryContainer = Color(0xFFF0E8FF),
    secondary = RgRokuAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF24183E),
    onSecondaryContainer = Color(0xFFF0E8FF),
    tertiary = RgDanger,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3A1720),
    onTertiaryContainer = Color(0xFFFFE4E9),
    background = RgBackground,
    onBackground = RgTextPrimary,
    surface = RgSurface,
    onSurface = RgTextPrimary,
    surfaceVariant = RgCard,
    onSurfaceVariant = RgTextSecondary,
    outline = Color(0xFF40345D),
    outlineVariant = Color(0xFF29243A),
    error = RgDanger,
    onError = Color.White
)

private val RgRemoteLightColors = lightColorScheme(
    primary = RgRokuPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DDFF),
    onPrimaryContainer = Color(0xFF250F56),
    secondary = RgRokuAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E2FF),
    onSecondaryContainer = Color(0xFF1F1440),
    tertiary = RgDanger,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E9),
    onTertiaryContainer = Color(0xFF3A1720),
    background = LightRgColors.background,
    onBackground = LightRgColors.textPrimary,
    surface = LightRgColors.surface,
    onSurface = LightRgColors.textPrimary,
    surfaceVariant = LightRgColors.card,
    onSurfaceVariant = LightRgColors.textSecondary,
    outline = Color(0xFF7C7690),
    outlineVariant = Color(0xFFD9DCE6),
    error = RgDanger,
    onError = Color.White
)

@Composable
fun RGRemoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val rgColors = if (dark) DarkRgColors else LightRgColors
    CompositionLocalProvider(LocalRgColors provides rgColors) {
        MaterialTheme(
            colorScheme = if (dark) RgRemoteDarkColors else RgRemoteLightColors,
            content = content
        )
    }
}
