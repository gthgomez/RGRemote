package com.rgremote.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Outline.Generic
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.roku.RokuPowerMode

internal val NeoBackground = Color(0xFF080B14)
internal val NeoSurface = Color(0xFF111522)
internal val NeoCard = Color(0xFF191E2C)
internal val PrimaryText = Color(0xFFF5F7FA)
internal val SecondaryText = Color(0xFFA7AFC1)
internal val SuccessGreen = Color(0xFF42F2A4)
internal val DangerRed = Color(0xFFFF667B)
internal val RokuPrimary = Color(0xFF8B5CFF)
internal val RokuAccent = Color(0xFFC7B6FF)
internal val GooglePrimary = Color(0xFF38BDF8)
internal val GoogleAccent = Color(0xFF7DD3FC)
internal val GlassStroke = Color.White.copy(alpha = 0.10f)
internal val PurpleGlow = Color(0xFF8B5CFF)
internal val NeoVioletDeep = Color(0xFF1A0F2E)
internal val NeoBlueAccent = Color(0xFF5B7CFF)

/** Octagonal D-pad plate shape (reference mockup). */
internal val OctagonPlateShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: androidx.compose.ui.unit.Density): Outline {
        val cut = minOf(size.width, size.height) * 0.22f
        val path = ComposePath().apply {
            moveTo(cut, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, cut)
            lineTo(size.width, size.height - cut)
            lineTo(size.width - cut, size.height)
            lineTo(cut, size.height)
            lineTo(0f, size.height - cut)
            lineTo(0f, cut)
            close()
        }
        return Generic(path)
    }
}

internal data class EcosystemStyle(
    val type: DeviceType,
    val label: String,
    val launchLabel: String,
    val primary: Color,
    val accent: Color
)

private val NeutralPrimary = Color(0xFF8B93A7)
private val NeutralAccent = Color(0xFFB4BAC8)

@Composable
internal fun rememberEcosystemStyle(deviceType: DeviceType?): EcosystemStyle =
    when (deviceType) {
        DeviceType.ROKU_TV -> EcosystemStyle(
            type = DeviceType.ROKU_TV,
            label = "Roku",
            launchLabel = "Launch Channel",
            primary = RokuPrimary,
            accent = RokuAccent
        )
        DeviceType.GOOGLE_TV -> EcosystemStyle(
            type = DeviceType.GOOGLE_TV,
            label = "Google TV",
            launchLabel = "Launch App",
            primary = GooglePrimary,
            accent = GoogleAccent
        )
        null -> EcosystemStyle(
            type = DeviceType.GOOGLE_TV,
            label = "Remote",
            launchLabel = "Launch",
            primary = NeutralPrimary,
            accent = NeutralAccent
        )
    }

internal fun setupGuideNeeded(state: RGRemoteUiState): Boolean {
    if (!state.showConnectionGuide) return false
    return !isSetupComplete(state)
}

/** Roku present, Google TV paired, and HDMI input mapped (dual-TV workflow ready). */
internal fun isSetupComplete(state: RGRemoteUiState): Boolean {
    if (state.rokuDevice == null) return false
    val google = state.googleTvDevice ?: return false
    return state.pairedDeviceIds.contains(google.id) && google.hdmiPortMapping != null
}

@Composable
internal fun NeoPanel(
    modifier: Modifier = Modifier,
    accent: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NeoSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .cyberEtch(accent, alpha = 0.08f)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

internal fun statusColor(status: ConnectionStatus, accent: Color): Color =
    when (status) {
        ConnectionStatus.ONLINE,
        ConnectionStatus.PAIRED -> SuccessGreen
        ConnectionStatus.CHECKING -> accent
        ConnectionStatus.OFFLINE,
        ConnectionStatus.NOT_PAIRED -> SecondaryText
        ConnectionStatus.CONNECTION_FAILED,
        ConnectionStatus.WAKE_UNAVAILABLE -> DangerRed
    }

internal fun rokuConnectionStatusLabel(state: RGRemoteUiState): String {
    if (state.selectedDevice?.type != DeviceType.ROKU_TV) return state.connectionStatus.label
    val power = state.selectedRokuPowerMode?.let { RokuPowerMode.displayLabel(it) }
    return if (power != null && state.connectionStatus == ConnectionStatus.ONLINE) {
        "${state.connectionStatus.label} · $power"
    } else {
        state.connectionStatus.label
    }
}

@Composable
internal fun RoundIconButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    size: Dp = 48.dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "roundIconPress")
    IconButton(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .neoGlow(accent, if (enabled) 0.24f else 0f, radius = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.11f else 0.04f),
                        accent.copy(alpha = if (enabled) 0.16f else 0.04f),
                        NeoCard.copy(alpha = 0.86f)
                    )
                )
            )
            .border(1.25.dp, accent.copy(alpha = if (enabled) 0.46f else 0.1f), RoundedCornerShape(24.dp)),
        enabled = enabled,
        interactionSource = interaction,
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) {
                accent
            } else {
                SecondaryText.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

internal fun Modifier.neoGlow(color: Color, alpha: Float, radius: Dp): Modifier =
    drawBehind {
        if (alpha <= 0f) return@drawBehind
        val strokeWidth = radius.toPx().coerceAtLeast(1f)
        drawRoundRect(
            color = color.copy(alpha = alpha),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

internal fun Modifier.cyberEtch(accent: Color, alpha: Float = 0.24f): Modifier =
    drawBehind {
        if (alpha <= 0f) return@drawBehind
        val stroke = 1.dp.toPx()
        val thin = accent.copy(alpha = alpha)
        val ghost = Color.White.copy(alpha = alpha * 0.25f)
        val w = size.width
        val h = size.height

        drawLine(thin, Offset(w * 0.58f, h * 0.13f), Offset(w * 0.72f, h * 0.13f), stroke)
        drawLine(thin, Offset(w * 0.72f, h * 0.13f), Offset(w * 0.78f, h * 0.20f), stroke)
        drawLine(thin, Offset(w * 0.78f, h * 0.20f), Offset(w * 0.92f, h * 0.20f), stroke)
        drawLine(thin, Offset(w * 0.06f, h * 0.72f), Offset(w * 0.22f, h * 0.72f), stroke)
        drawLine(thin, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.29f, h * 0.64f), stroke)
        drawLine(thin, Offset(w * 0.29f, h * 0.64f), Offset(w * 0.39f, h * 0.64f), stroke)
        drawLine(ghost, Offset(w * 0.04f, h * 0.32f), Offset(w * 0.50f, h * 0.32f), stroke)
        drawLine(ghost, Offset(w * 0.66f, h * 0.82f), Offset(w * 0.94f, h * 0.82f), stroke)
        drawCircle(thin, radius = 1.5.dp.toPx(), center = Offset(w * 0.78f, h * 0.20f))
        drawCircle(thin, radius = 1.5.dp.toPx(), center = Offset(w * 0.29f, h * 0.64f))
    }

/** Faint PCB-style grid behind the whole screen (reference mockup). */
internal fun Modifier.circuitBoardBackground(accent: Color = PurpleGlow): Modifier =
    drawBehind {
        val line = accent.copy(alpha = 0.04f)
        val dot = accent.copy(alpha = 0.10f)
        val step = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), 0.5f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 0.5f)
            y += step
        }
        drawCircle(dot, radius = 1.2f, center = Offset(size.width * 0.18f, size.height * 0.22f))
        drawCircle(dot, radius = 1.2f, center = Offset(size.width * 0.82f, size.height * 0.68f))
    }

/** Soft radial glow behind the hero remote card. */
internal fun Modifier.remoteHeroAmbient(accent: Color, strength: Float = 0.35f): Modifier =
    drawBehind {
        if (strength <= 0f) return@drawBehind
        drawCircle(
            color = accent.copy(alpha = strength * 0.22f),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.42f, size.height * 0.38f)
        )
        drawCircle(
            color = NeoBlueAccent.copy(alpha = strength * 0.08f),
            radius = size.minDimension * 0.48f,
            center = Offset(size.width * 0.72f, size.height * 0.55f)
        )
    }

internal fun Modifier.iridescentBorder(accent: Color, width: Dp = 1.25.dp, cornerRadius: Dp = 28.dp): Modifier =
    drawBehind {
        val stroke = width.toPx()
        val radius = cornerRadius.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0.85f),
                    NeoBlueAccent.copy(alpha = 0.45f),
                    accent.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.12f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )
    }
