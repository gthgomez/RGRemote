package com.rgremote.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RgRemoteColors = darkColorScheme(
    primary = Color(0xFF8B5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF321D68),
    onPrimaryContainer = Color(0xFFF0E8FF),
    secondary = Color(0xFFC7B6FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF24183E),
    onSecondaryContainer = Color(0xFFF0E8FF),
    tertiary = Color(0xFFFF5D73),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3A1720),
    onTertiaryContainer = Color(0xFFFFE4E9),
    background = Color(0xFF080B14),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF111522),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF191E2C),
    onSurfaceVariant = Color(0xFFA7AFC1),
    outline = Color(0xFF40345D),
    outlineVariant = Color(0xFF29243A),
    error = Color(0xFFFF5D73),
    onError = Color.White
)

@Composable
fun RGRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RgRemoteColors,
        content = content
    )
}
