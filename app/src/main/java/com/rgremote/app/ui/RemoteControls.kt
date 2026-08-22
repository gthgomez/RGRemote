package com.rgremote.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.roku.RokuPowerMode

@Composable
internal fun RemoteSurface(
    modifier: Modifier = Modifier,
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    enabled: Boolean,
    powerEnabled: Boolean,
    rokuControls: RokuControlAvailability,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    onSelect: (String) -> Unit,
    onGoogleSelect: (com.rgremote.app.domain.RegisteredDevice) -> Unit,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onDpad: (DpadDirection) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (VolumeCommand) -> Unit,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
) {
    val selected = state.selectedDevice
    val rokuPowerMode = if (ecosystem.type == DeviceType.ROKU_TV) state.selectedRokuPowerMode else null
    val powerOffEnabled = powerEnabled && (ecosystem.type != DeviceType.ROKU_TV || rokuControls.supportsPowerOff != false)
    val volumeEnabled = enabled && (ecosystem.type != DeviceType.ROKU_TV || rokuControls.supportsVolume != false)
    val wakeHint = if (ecosystem.type == DeviceType.ROKU_TV) RokuPowerMode.wakeHint(rokuPowerMode) else null
    val heroGlow = if (ecosystem.type == DeviceType.ROKU_TV) 0.42f else 0.28f
    val hapticTrigger = rememberHapticTrigger()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .remoteHeroAmbient(accent, heroGlow)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .iridescentBorder(accent, width = 1.5.dp, cornerRadius = 30.dp),
            shape = RoundedCornerShape(30.dp),
            color = NeoSurface.copy(alpha = 0.90f),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NeoVioletDeep.copy(alpha = 0.92f),
                                NeoSurface.copy(alpha = 0.98f),
                                Color(0xFF060A14)
                            )
                        )
                    )
                    .cyberEtch(accent, alpha = if (ecosystem.type == DeviceType.ROKU_TV) 0.32f else 0.12f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Device Name, Connection Status, and Overflow actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selected?.friendlyName ?: "No device selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor(state.connectionStatus, accent))
                            )
                            Text(
                                text = if (selected?.type == DeviceType.ROKU_TV) {
                                    rokuConnectionStatusLabel(state)
                                } else {
                                    state.connectionStatus.label
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor(state.connectionStatus, accent),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Device Actions",
                                tint = PrimaryText.copy(alpha = 0.8f)
                            )
                        }
                        HeaderOverflowMenu(
                            expanded = menuExpanded,
                            state = state,
                            selected = selected,
                            onDismiss = { menuExpanded = false },
                            onScan = onScan,
                            onRefreshStatus = onRefreshStatus,
                            onOpenSetupGuide = onOpenSetupGuide
                        )
                    }
                }

                EcosystemSegmentBar(
                    state = state,
                    selected = selected,
                    onSelect = onSelect,
                    onGoogleSelect = onGoogleSelect
                )

                val isStandby = RokuPowerMode.displayLabel(rokuPowerMode) == "Standby" ||
                        state.connectionStatus == ConnectionStatus.OFFLINE
                val powerModeUnknown = ecosystem.type == DeviceType.ROKU_TV && rokuPowerMode == null &&
                        state.connectionStatus != ConnectionStatus.OFFLINE

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", accentSoft, enabled) {
                        hapticTrigger { onCommand(RemoteCommand.Back) }
                    }
                    RoundIconButton(Icons.Default.Home, "Home", accentSoft, enabled) {
                        hapticTrigger { onCommand(RemoteCommand.Home) }
                    }
                    val powerCommand = when {
                        ecosystem.type != DeviceType.ROKU_TV -> RemoteCommand.PowerToggle
                        powerModeUnknown -> RemoteCommand.PowerToggle
                        isStandby -> RemoteCommand.PowerOn
                        else -> RemoteCommand.PowerOff
                    }
                    val powerAccent = when {
                        ecosystem.type != DeviceType.ROKU_TV -> DangerRed
                        powerModeUnknown -> SecondaryText
                        isStandby -> SuccessGreen
                        else -> DangerRed
                    }
                    val powerControlEnabled =
                        if (ecosystem.type == DeviceType.ROKU_TV && isStandby) powerEnabled else powerOffEnabled
                    RoundIconButton(
                        icon = Icons.Default.PowerSettingsNew,
                        label = if (!powerModeUnknown && isStandby) "Wake" else "Power",
                        accent = powerAccent,
                        enabled = powerControlEnabled
                    ) {
                        hapticTrigger { onCommand(powerCommand) }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                ) {
                    val clusterCap = minOf(
                        maxWidth * 0.92f,
                        maxHeight * 0.96f,
                        320.dp,
                    ).coerceAtLeast(180.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val clusterSize = minOf(maxWidth, clusterCap).coerceAtLeast(180.dp)
                            DpadCluster(
                                ecosystem = ecosystem,
                                accent = accent,
                                accentSoft = accentSoft,
                                enabled = enabled,
                                clusterSize = clusterSize,
                                onDpad = { direction ->
                                    hapticTrigger { onDpad(direction) }
                                },
                                onSelect = {
                                    hapticTrigger { onCommand(RemoteCommand.Select) }
                                }
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VolumeRail(
                                accent = accent,
                                enabled = volumeEnabled,
                                height = (clusterCap - 48.dp).coerceAtLeast(120.dp),
                                onVolumeUp = { hapticTrigger { onVolume(VolumeCommand.UP) } },
                                onVolumeDown = { hapticTrigger { onVolume(VolumeCommand.DOWN) } }
                            )
                            RoundIconButton(
                                icon = Icons.AutoMirrored.Filled.VolumeOff,
                                label = "Mute",
                                accent = accentSoft,
                                enabled = volumeEnabled,
                                size = 40.dp
                            ) {
                                hapticTrigger { onVolume(VolumeCommand.MUTE) }
                            }
                        }
                    }
                }

                TransportButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow,
                    label = "PLAY",
                    accent = accentSoft,
                    enabled = enabled,
                    emphasized = false
                ) {
                    hapticTrigger { onCommand(RemoteCommand.PlayPause) }
                }

                if (wakeHint != null && ecosystem.type == DeviceType.ROKU_TV) {
                    Text(
                        text = wakeHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }

                if (showWatchGoogleTv || showWatchRoku) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showWatchGoogleTv) {
                            CompactWatchChip(
                                modifier = Modifier.weight(1f),
                                label = "Google TV",
                                accent = GooglePrimary,
                                onClick = onWatchGoogleTv
                            )
                        }
                        if (showWatchRoku) {
                            CompactWatchChip(
                                modifier = Modifier.weight(1f),
                                label = "Roku",
                                accent = RokuPrimary,
                                onClick = onWatchRoku
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWatchChip(
    modifier: Modifier,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = PrimaryText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun VolumeRail(
    accent: Color,
    enabled: Boolean,
    height: Dp = 168.dp,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
) {
    val railWidth = (height * 0.34f).coerceIn(58.dp, 72.dp)
    val buttonSize = (height * 0.28f).coerceIn(48.dp, 56.dp)
    Surface(
        modifier = Modifier
            .width(railWidth)
            .height(height)
            .neoGlow(accent, if (enabled) 0.20f else 0f, radius = 20.dp),
        shape = RoundedCornerShape(26.dp),
        color = NeoCard.copy(alpha = if (enabled) 0.82f else 0.36f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.42f else 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Volume up",
                accent = accent,
                enabled = enabled,
                size = buttonSize,
                holdRepeat = true,
                onClick = onVolumeUp
            )
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume",
                tint = accent.copy(alpha = if (enabled) 0.55f else 0.25f),
                modifier = Modifier.size(buttonSize * 0.5f)
            )
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeDown,
                label = "Volume down",
                accent = accent,
                enabled = enabled,
                size = buttonSize,
                holdRepeat = true,
                onClick = onVolumeDown
            )
        }
    }
}

@Composable
internal fun SurfacePill(modifier: Modifier = Modifier, text: String, accent: Color) {
    Surface(
        modifier = modifier.padding(end = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = NeoCard.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .neoGlow(accent, 0.6f, radius = 8.dp)
            )
            Text(
                text,
                color = PrimaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun DpadCluster(
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    enabled: Boolean,
    clusterSize: Dp,
    onDpad: (DpadDirection) -> Unit,
    onSelect: () -> Unit
) {
    val isRoku = ecosystem.type == DeviceType.ROKU_TV
    val directionButtonSize = (clusterSize * 0.24f).coerceIn(52.dp, 66.dp)
    val okDiameter = (clusterSize * 0.38f).coerceIn(80.dp, 104.dp)
    val baseSize by animateDpAsState(
        targetValue = clusterSize - if (isRoku) 8.dp else 16.dp,
        animationSpec = tween(durationMillis = 260),
        label = "dpadBaseSize"
    )
    val baseShape: Shape = if (isRoku) OctagonPlateShape else RoundedCornerShape(52.dp)

    Box(
        modifier = Modifier.size(clusterSize),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(baseSize)
                .neoGlow(accent, if (enabled) 0.38f else 0f, radius = 28.dp),
            shape = baseShape,
            color = NeoCard.copy(alpha = 0.90f),
            border = BorderStroke(1.75.dp, accent.copy(alpha = if (enabled) 0.62f else 0.12f))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (isRoku) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.28f),
                                        accent.copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.42f
                                ),
                                radius = size.minDimension * 0.42f,
                                center = center
                            )
                            val line = Color.White.copy(alpha = 0.10f)
                            drawLine(line, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())
                            drawLine(line, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
                        }
                    }
            )
        }
        val edgePad = if (isRoku) 4.dp else 10.dp
        DpadButton(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = edgePad),
            icon = Icons.Default.KeyboardArrowUp,
            label = "Up",
            accent = accentSoft,
            enabled = enabled,
            compact = true,
            faceplate = false,
            buttonSize = directionButtonSize,
            holdRepeat = true,
        ) { onDpad(DpadDirection.UP) }
        DpadButton(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = edgePad),
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = "Left",
            accent = accentSoft,
            enabled = enabled,
            compact = true,
            faceplate = isRoku,
            buttonSize = directionButtonSize,
            holdRepeat = true,
        ) { onDpad(DpadDirection.LEFT) }
        DpadButton(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = edgePad),
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            label = "Right",
            accent = accentSoft,
            enabled = enabled,
            compact = true,
            faceplate = isRoku,
            buttonSize = directionButtonSize,
            holdRepeat = true,
        ) { onDpad(DpadDirection.RIGHT) }
        DpadButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = edgePad),
            icon = Icons.Default.KeyboardArrowDown,
            label = "Down",
            accent = accentSoft,
            enabled = enabled,
            compact = true,
            faceplate = false,
            buttonSize = directionButtonSize,
            holdRepeat = true,
        ) { onDpad(DpadDirection.DOWN) }
        OkButton(accent = accent, enabled = enabled, diameter = okDiameter, onClick = onSelect)
    }
}

@Composable
internal fun DpadButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    compact: Boolean,
    faceplate: Boolean = false,
    buttonSize: Dp = if (compact) 50.dp else 52.dp,
    holdRepeat: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val size by animateDpAsState(
        targetValue = buttonSize,
        animationSpec = tween(durationMillis = 260),
        label = "dpadButtonSize"
    )
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "dpadButtonPress")
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.28f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "dpadButtonGlow"
    )
    IconButton(
        modifier = modifier
            .size(if (faceplate) size + 6.dp else size)
            .then(
                if (holdRepeat) {
                    Modifier.holdRepeatPress(enabled, interaction, onClick)
                } else {
                    Modifier
                }
            )
            .scale(scale)
            .neoGlow(accent, glowAlpha, radius = 14.dp)
            .clip(if (faceplate) RoundedCornerShape(14.dp) else CircleShape)
            .background(if (faceplate) Color.Black.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                1.dp,
                if (faceplate) accent.copy(alpha = 0.22f) else Color.Transparent,
                if (faceplate) RoundedCornerShape(14.dp) else CircleShape
            ),
        enabled = enabled,
        interactionSource = interaction,
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) Color(0xFFE6DDFF) else SecondaryText.copy(alpha = 0.42f),
            modifier = Modifier.size(size * 0.58f)
        )
    }
}

@Composable
internal fun OkButton(
    accent: Color,
    enabled: Boolean,
    diameter: Dp = 76.dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val size by animateDpAsState(
        targetValue = diameter,
        animationSpec = tween(durationMillis = 280),
        label = "okSize"
    )
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "okPress")
    Button(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .neoGlow(accent, if (enabled) 0.55f else 0f, radius = 24.dp),
        enabled = enabled,
        interactionSource = interaction,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.32f),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF151923),
            disabledContentColor = SecondaryText.copy(alpha = 0.4f)
        ),
        border = BorderStroke(2.dp, accent.copy(alpha = if (enabled) 0.95f else 0.18f)),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Text("OK", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TransportButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val glow = if (emphasized && enabled) 0.28f else if (enabled) 0.10f else 0f
    Button(
        modifier = modifier
            .height(56.dp)
            .neoGlow(accent, glow, radius = 18.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = if (enabled) (if (emphasized) 0.22f else 0.14f) else 0.06f),
            contentColor = if (enabled) accent else SecondaryText.copy(alpha = 0.42f),
            disabledContainerColor = NeoCard.copy(alpha = 0.40f),
            disabledContentColor = SecondaryText.copy(alpha = 0.42f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) (if (emphasized) 0.45f else 0.28f) else 0.1f)),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
