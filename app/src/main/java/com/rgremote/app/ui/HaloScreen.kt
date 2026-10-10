package com.rgremote.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rgremote.app.R
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.ui.ringGradientFill
import com.rgremote.app.ui.theme.HaloColors
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.ringGradientColors
import com.rgremote.app.ui.theme.rememberRingBrush
import com.rgremote.app.ui.theme.LocalHaloColors

/**
 * The halo remote screen: wordmark header, device chip, halo ring D-pad, glass
 * dock, and the More Controls affordance — the visual contract in
 * `docs/ui-target.md`.
 */
@Composable
internal fun HaloRemoteScreen(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    controlsEnabled: Boolean,
    volumeEnabled: Boolean,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onSelect: (String) -> Unit,
    onGoogleSelect: (RegisteredDevice) -> Unit,
    onBeginDpadSequence: () -> Long,
    onDpadRepeat: (DpadDirection, Long) -> Unit,
    onEndDpadSequence: () -> Unit,
    onSelectCommand: () -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (com.rgremote.app.domain.VolumeCommand) -> Unit,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (com.rgremote.app.domain.AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (com.rgremote.app.domain.HdmiPort) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardEnter: () -> Unit,
    onKeyboardBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val halo = LocalHaloColors.current
    val selected = state.selectedDevice
    val rokuPowerMode = if (ecosystem.type == com.rgremote.app.domain.DeviceType.ROKU_TV) {
        state.selectedRokuPowerMode
    } else {
        null
    }
    val power = resolvePowerPresentation(ecosystem.type, rokuPowerMode, state.connectionStatus)
    val powerOffEnabled = selected != null &&
            (ecosystem.type != com.rgremote.app.domain.DeviceType.ROKU_TV ||
                state.selectedRokuControls.supportsPowerOff != false)
    val powerEnabled = if (power.isWake) selected != null else powerOffEnabled

    var showSwitcher by remember { mutableStateOf(false) }
    var showMoreControls by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var dpadGeneration by remember { mutableStateOf(0L) }

    // Screen-wide "awake" state: any touch anywhere wakes the secondary
    // controls (+/- rail, glass dock); after ScreenAwakeLingerMillis of no
    // touch they fade. The dock dims and stops responding while asleep.
    var controlsAwake by remember { mutableStateOf(true) }
    var awakeHideJob by remember { mutableStateOf<Job?>(null) }
    val wakeScope = rememberCoroutineScope()
    val wakeControls = {
        awakeHideJob?.cancel()
        controlsAwake = true
        awakeHideJob = wakeScope.launch {
            delay(ScreenAwakeLingerMillis)
            controlsAwake = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Fade the secondary controls after the initial idle window.
        wakeControls()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Passive wake listener: sees every touch before children on the
            // Initial pass, never consumes, so children behave unchanged.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    wakeControls()
                }
            }
    ) {
        HaloBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HaloSpec.ScreenHorizontalPaddingDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HaloTopBar(
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                state = state,
                selected = selected,
                onScan = onScan,
                onRefreshStatus = onRefreshStatus,
                onOpenSetupGuide = onOpenSetupGuide
            )
            Spacer(Modifier.height(10.dp))
            HaloDeviceChip(
                state = state,
                enabled = true,
                onClick = { showSwitcher = true }
            )
            Spacer(Modifier.weight(0.4f))
            BoxWithConstraints {
                val ringDiameter = minOf(
                    maxWidth * HaloSpec.RingWidthFraction,
                    maxHeight * 0.96f,
                    HaloSpec.RingMaxDiameterDp.dp
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
            HaloDpad(
                enabled = controlsEnabled,
                onSequenceStart = { dpadGeneration = onBeginDpadSequence() },
                onPress = { action ->
                    when (action) {
                        RingAction.SELECT -> onSelectCommand()
                        RingAction.UP -> onDpadRepeat(DpadDirection.UP, dpadGeneration)
                        RingAction.DOWN -> onDpadRepeat(DpadDirection.DOWN, dpadGeneration)
                        RingAction.LEFT -> onDpadRepeat(DpadDirection.LEFT, dpadGeneration)
                        RingAction.RIGHT -> onDpadRepeat(DpadDirection.RIGHT, dpadGeneration)
                    }
                },
                onSequenceEnd = onEndDpadSequence,
                modifier = Modifier.width(ringDiameter)
                )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsAwake,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        RingVolumeRail(
                            enabled = volumeEnabled,
                            onVolume = onVolume
                        )
                    }
                }
            }
            Spacer(Modifier.weight(0.4f))
            GlassActionDock(
                enabled = controlsEnabled,
                awake = controlsAwake,
                power = power,
                powerEnabled = powerEnabled,
                onBack = { onCommand(RemoteCommand.Back) },
                onHome = { onCommand(RemoteCommand.Home) },
                onPower = { onCommand(power.command) },
                modifier = Modifier.fillMaxWidth(HaloSpec.DockWidthFraction)
            )
            Spacer(Modifier.height(14.dp))
            MoreControlsAffordance(
                enabled = !showMoreControls,
                onClick = { showMoreControls = true }
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showSwitcher) {
        HaloDeviceSwitcherSheet(
            state = state,
            onSelect = onSelect,
            onGoogleSelect = onGoogleSelect,
            onDismiss = { showSwitcher = false }
        )
    }
    if (showMoreControls) {
        MoreControlsSheet(
            state = state,
            ecosystem = ecosystem,
            accent = accent,
            accentSoft = accentSoft,
            volumeEnabled = volumeEnabled && controlsEnabled,
            onVolume = onVolume,
            showWatchGoogleTv = showWatchGoogleTv,
            showWatchRoku = showWatchRoku,
            onWatchGoogleTv = onWatchGoogleTv,
            onWatchRoku = onWatchRoku,
            onLaunchPreset = onLaunchPreset,
            onLaunchPinnedApp = onLaunchPinnedApp,
            onSelectTab = {
                showMoreControls = false
                onSelectTab(it)
            },
            onTargetChange = onTargetChange,
            onLaunch = onLaunch,
            onSwitchInput = onSwitchInput,
            onSendText = onSendText,
            onKeyboardEnter = onKeyboardEnter,
            onKeyboardBackspace = onKeyboardBackspace,
            onDismiss = { showMoreControls = false }
        )
    }
}

/** Indigo-to-vignette background with the mockup's ambient blooms. */
@Composable
internal fun HaloBackdrop() {
    val halo = LocalHaloColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(halo.backgroundTop, halo.backgroundBase, halo.backgroundDeep)
                )
            )
            .drawBehind {
                val sizePx = size
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(halo.bloomViolet.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(0f, sizePx.height * 0.62f),
                        radius = sizePx.width * 0.9f
                    ),
                    radius = sizePx.width * 0.9f,
                    center = Offset(0f, sizePx.height * 0.62f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(halo.bloomBlue.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(sizePx.width, sizePx.height * 0.40f),
                        radius = sizePx.width * 0.85f
                    ),
                    radius = sizePx.width * 0.85f,
                    center = Offset(sizePx.width, sizePx.height * 0.40f)
                )
                if (halo.bloomPink != Color.Transparent) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(halo.bloomPink.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(sizePx.width / 2f, sizePx.height * 0.88f),
                            radius = sizePx.width * 0.5f
                        ),
                        radius = sizePx.width * 0.5f,
                        center = Offset(sizePx.width / 2f, sizePx.height * 0.88f)
                    )
                }
            }
    )
}

@Composable
internal fun HaloTopBar(
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    state: RGRemoteUiState,
    selected: RegisteredDevice?,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
) {
    val halo = LocalHaloColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HaloSpec.HeaderHeightDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RG",
            color = halo.wordmarkAccent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Remote",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(halo.dockWell, CircleShape)
                    .clickable(
                        onClickLabel = stringResource(R.string.remote_cd_device_actions)
                    ) { onMenuToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = halo.dockIcon
                )
            }
            HeaderOverflowMenu(
                expanded = menuExpanded,
                state = state,
                selected = selected,
                onDismiss = { onMenuToggle(false) },
                onScan = onScan,
                onRefreshStatus = onRefreshStatus,
                onOpenSetupGuide = onOpenSetupGuide
            )
        }
    }
}

@Composable
internal fun haloStatusColor(status: ConnectionStatus, halo: HaloColors = LocalHaloColors.current): Color =
    when (status) {
        ConnectionStatus.ONLINE -> halo.statusOnline
        ConnectionStatus.CHECKING -> halo.statusChecking
        ConnectionStatus.NOT_PAIRED, ConnectionStatus.PAIRED -> halo.statusUnpaired
        else -> halo.statusOffline
    }

@Composable
internal fun HaloDeviceChip(
    state: RGRemoteUiState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val halo = LocalHaloColors.current
    val selected = state.selectedDevice
    val baseLabel = if (selected?.type == com.rgremote.app.domain.DeviceType.ROKU_TV) {
        rokuConnectionStatusLabel(state)
    } else {
        connectionStatusText(state.connectionStatus)
    }
    val statusLabel = if (state.connectionStatus == ConnectionStatus.NOT_PAIRED) {
        stringResource(R.string.halo_not_paired_hint)
    } else {
        baseLabel
    }
    val shape = RoundedCornerShape(HaloSpec.ChipHeightDp.dp / 2f - 4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HaloSpec.ChipHeightDp.dp)
            .background(halo.chipFill, shape)
            .border(1.dp, halo.chipBorder, shape)
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled, onClickLabel = statusLabel, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(halo.wordmarkAccent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Tv,
                contentDescription = null,
                tint = halo.wordmarkAccent
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = selected?.friendlyName
                ?: stringResource(R.string.remote_no_device_selected),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .width(1.dp)
                .height(20.dp)
                .background(halo.dockSeparator)
        )
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f, fill = true),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(haloStatusColor(state.connectionStatus), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = statusLabel,
                color = haloStatusColor(state.connectionStatus),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.halo_cd_switch_device),
            tint = halo.dockIcon
        )
    }
}

@Composable
private fun MoreControlsAffordance(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val halo = LocalHaloColors.current
    val ringBrush = rememberRingBrush()
    val density = LocalDensity.current
    val swipeUpThresholdPx = with(density) { SwipeUpThresholdDp.toPx() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClickLabel = stringResource(R.string.halo_more_controls),
                onClick = onClick
            )
            // Upward swipe on the affordance also opens the sheet, so the
            // gesture lives here rather than on the navigation ring.
            .pointerInput(enabled, swipeUpThresholdPx) {
                if (!enabled) return@pointerInput
                var totalDrag = 0f
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        triggered = false
                    },
                    onDragEnd = {
                        totalDrag = 0f
                        triggered = false
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        triggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        if (!triggered && totalDrag < -swipeUpThresholdPx) {
                            triggered = true
                            change.consume()
                            onClick()
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(halo.dockWell, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.ringGradientFill(halo.ringGradientColors())
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.halo_more_controls),
            style = MaterialTheme.typography.labelSmall.copy(brush = ringBrush),
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
    }
}

private val SwipeUpThresholdDp = 24.dp

/**
 * The mockup's +/− volume controls flanking the ring's right edge.
 */
@Composable
private fun RingVolumeRail(
    enabled: Boolean,
    onVolume: (com.rgremote.app.domain.VolumeCommand) -> Unit,
) {
    val halo = LocalHaloColors.current
    Column(
        modifier = Modifier.padding(end = 2.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .alpha(if (enabled) 1f else 0.35f)
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.remote_volume_up)
                ) { onVolume(com.rgremote.app.domain.VolumeCommand.UP) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.ringGradientFill(halo.ringGradientColors())
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .alpha(if (enabled) 1f else 0.35f)
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.remote_volume_down)
                ) { onVolume(com.rgremote.app.domain.VolumeCommand.DOWN) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.ringGradientFill(halo.ringGradientColors())
            )
        }
    }
}

private const val ScreenAwakeLingerMillis = 4000L
