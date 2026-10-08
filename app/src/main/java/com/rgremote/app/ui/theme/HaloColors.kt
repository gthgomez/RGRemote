package com.rgremote.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Halo remote visual contract tokens, sampled from the reference mockups in
 * `docs/reference/` (see `docs/ui-target.md`).
 *
 * Everything here flips between dark and light. Theme-invariant accents
 * (RgSuccess, RgDanger, ecosystem colors) stay in [Palette.kt]. UI code must
 * reference these tokens instead of inline Color(0x...) literals.
 */
@Immutable
data class HaloColors(
    // Screen background gradient (top -> base) and deep vignette edge.
    val backgroundTop: Color,
    val backgroundBase: Color,
    val backgroundDeep: Color,
    // Ambient blooms drawn behind the ring and dock.
    val bloomViolet: Color,
    val bloomBlue: Color,
    val bloomPink: Color,
    // Wordmark accent ("RG").
    val wordmarkAccent: Color,
    // Ring sweep gradient stops: light blue at top -> violet -> pink at bottom.
    val ringTop: Color,
    val ringMid: Color,
    val ringBottom: Color,
    val ringPink: Color,
    // Band between the glowing ring and the center disc, and the disc itself.
    val ringBandFill: Color,
    val ringCenterFill: Color,
    val ringCenterBorder: Color,
    // Pressed-state accents (direction arrow, select pulse, volume rail).
    val ringAccent: Color,
    // Device chip glass.
    val chipFill: Color,
    val chipBorder: Color,
    // Glass action dock.
    val dockFill: Color,
    val dockBorder: Color,
    val dockSeparator: Color,
    val dockWell: Color,
    val dockIcon: Color,
    val powerWellTint: Color,
    // More Controls affordance and sheet.
    val moreControls: Color,
    // Connection status dot/label colors (contrast-tuned per theme).
    val statusOnline: Color,
    val statusChecking: Color,
    val statusOffline: Color,
    val statusUnpaired: Color,
)

val DarkHaloColors = HaloColors(
    backgroundTop = Color(0xFF0B0F27),
    backgroundBase = Color(0xFF0B0F27),
    backgroundDeep = Color(0xFF070A1E),
    bloomViolet = Color(0xFF5533AD),
    bloomBlue = Color(0xFF1C53C9),
    bloomPink = Color(0x00000000),
    wordmarkAccent = Color(0xFFA26DF6),
    ringTop = Color(0xFF4FB8FF),
    ringMid = Color(0xFF6D7CFF),
    ringBottom = Color(0xFF9B5CF6),
    ringPink = Color(0xFFD86DDF),
    ringBandFill = Color(0xFF131A40),
    ringCenterFill = Color(0xFF0C112E),
    ringCenterBorder = Color.White.copy(alpha = 0.08f),
    ringAccent = Color(0xFFBFE4FF),
    chipFill = Color(0xFF14183A).copy(alpha = 0.85f),
    chipBorder = Color.White.copy(alpha = 0.10f),
    dockFill = Color(0xFF161B44).copy(alpha = 0.78f),
    dockBorder = Color.White.copy(alpha = 0.10f),
    dockSeparator = Color.White.copy(alpha = 0.08f),
    dockWell = Color(0xFF1C2350),
    dockIcon = RgDpadIcon,
    powerWellTint = RgDanger.copy(alpha = 0.16f),
    moreControls = RgTextSecondary,
    statusOnline = RgSuccess,
    statusChecking = Color(0xFFF5C044),
    statusOffline = RgNeutralPrimary,
    statusUnpaired = RgRokuAccent,
)

val LightHaloColors = HaloColors(
    backgroundTop = Color(0xFFF4F7FF),
    backgroundBase = Color(0xFFD8E8F8),
    backgroundDeep = Color(0xFFCBDCEE),
    bloomViolet = Color(0xFF9CB4FC),
    bloomBlue = Color(0xFFB4E4FC),
    bloomPink = Color(0xFFFFC1D0),
    wordmarkAccent = Color(0xFF6D3FD6),
    ringTop = Color(0xFFA8D8FF),
    ringMid = Color(0xFFB4C0FF),
    ringBottom = Color(0xFFC9A6F5),
    ringPink = Color(0xFFF2BEE8),
    ringBandFill = Color(0xFFEDF1FC),
    ringCenterFill = Color.White,
    ringCenterBorder = Color(0xFFC9C9F5),
    ringAccent = Color(0xFF3D55C8),
    chipFill = Color.White,
    chipBorder = Color(0xFFE1E6F5),
    dockFill = Color.White.copy(alpha = 0.92f),
    dockBorder = Color(0xFFE1E6F5),
    dockSeparator = Color(0xFFD9DEF0),
    dockWell = Color(0xFFF4F6FE),
    dockIcon = Color(0xFF3A4258),
    powerWellTint = Color(0xFFFFC1D0),
    moreControls = Color(0xFF5A6478),
    statusOnline = Color(0xFF1E9E62),
    statusChecking = Color(0xFF9A6A00),
    statusOffline = Color(0xFF5A6478),
    statusUnpaired = Color(0xFF6D3FD6),
)

val LocalHaloColors = staticCompositionLocalOf { DarkHaloColors }

/**
 * Measured geometry contract from `docs/ui-target.md`. Fractions are relative
 * to screen width (ring) or parent size (the rest); components must consume
 * these instead of re-declaring dimensions.
 */
object HaloSpec {
    /** Ring outer diameter as a fraction of screen width (~290dp at 412dp). */
    const val RingWidthFraction = 0.72f

    /** Ring outer diameter cap in dp for tablets/large screens. */
    const val RingMaxDiameterDp = 420f

    /** Center select disc diameter as a fraction of the ring diameter. */
    const val CenterDiscFraction = 0.60f

    /** Ring bright core stroke width as a fraction of the ring diameter (~40dp at 290dp). */
    const val RingCoreStrokeFraction = 0.14f

    /** Select zone radius as a fraction of the ring radius (hit-testing contract). */
    const val SelectZoneRadiusFraction = 0.58f

    /** Screen horizontal padding in dp. */
    const val ScreenHorizontalPaddingDp = 20f

    /** Header height in dp. */
    const val HeaderHeightDp = 50f

    /** Device chip height in dp. */
    const val ChipHeightDp = 52f

    /** Dock capsule height in dp. */
    const val DockHeightDp = 76f

    /** Dock width as a fraction of screen width. */
    const val DockWidthFraction = 0.80f

    /** Minimum circular dock well diameter in dp. */
    const val DockWellDp = 52f

    /** Minimum touch target for the More Controls affordance in dp. */
    const val MoreControlsMinTargetDp = 48f

    /** Circular well diameter for sheet volume controls in dp. */
    const val SheetWellDp = 56f
}
