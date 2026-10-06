package com.rgremote.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
private val RgRemoteColors = darkColorScheme(
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

@Composable
fun RGRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RgRemoteColors,
        content = content
    )
}
