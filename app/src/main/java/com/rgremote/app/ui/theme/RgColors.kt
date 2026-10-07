package com.rgremote.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-dependent semantic colors. Ecosystem accents (Roku purple, Google blue)
 * and success/danger colors are intentionally constant across themes and stay
 * in [Palette.kt]; everything that must flip between dark and light lives here.
 */
@Immutable
data class RgColors(
    val background: Color,
    val backgroundDeep: Color,
    val backgroundLow: Color,
    val surface: Color,
    val card: Color,
    val innerCard: Color,
    val bottomBar: Color,
    val violetDeep: Color,
    val surfaceDeep: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dpadIcon: Color,
    val glassStroke: Color,
)

val DarkRgColors = RgColors(
    background = Color(0xFF080B14),
    backgroundDeep = Color(0xFF030713),
    backgroundLow = Color(0xFF0A1020),
    surface = Color(0xFF111522),
    card = Color(0xFF191E2C),
    innerCard = Color(0xFF151922),
    bottomBar = Color(0xF20B101C),
    violetDeep = Color(0xFF1A0F2E),
    surfaceDeep = Color(0xFF060A14),
    textPrimary = Color(0xFFF5F7FA),
    textSecondary = Color(0xFFA7AFC1),
    dpadIcon = Color(0xFFE6DDFF),
    glassStroke = Color.White.copy(alpha = 0.10f),
)

val LightRgColors = RgColors(
    background = Color(0xFFF2F4F9),
    backgroundDeep = Color(0xFFE4E9F2),
    backgroundLow = Color(0xFFF8FAFE),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFF1F3FA),
    innerCard = Color(0xFFF6F8FC),
    bottomBar = Color(0xF5FFFFFF),
    violetDeep = Color(0xFFEDE7FB),
    surfaceDeep = Color(0xFFE7ECF5),
    textPrimary = Color(0xFF1A2030),
    textSecondary = Color(0xFF5A6478),
    dpadIcon = Color(0xFF3A2E5C),
    glassStroke = Color.Black.copy(alpha = 0.10f),
)

val LocalRgColors = staticCompositionLocalOf { DarkRgColors }
