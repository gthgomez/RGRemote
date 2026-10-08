package com.rgremote.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rgremote.app.R
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand

/** Width at which the Remote tab switches from stacked phone layout to two-pane. */
private val TwoPaneMinWidth = 600.dp

@Composable
internal fun RemoteTabLayout(
    modifier: Modifier = Modifier,
    viewportHeight: Dp,
    sectionSpacing: Dp,
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onSelect: (String) -> Unit,
    onGoogleSelect: (com.rgremote.app.domain.RegisteredDevice) -> Unit,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onDpad: (DpadDirection) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (VolumeCommand) -> Unit,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (HdmiPort) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardEnter: () -> Unit,
    onKeyboardBackspace: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    val deviceDown = state.connectionStatus == ConnectionStatus.OFFLINE ||
            state.connectionStatus == ConnectionStatus.CONNECTION_FAILED
    val controlsEnabled = state.selectedDevice != null && !deviceDown

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= TwoPaneMinWidth) {
            TwoPaneContent(
                state = state,
                ecosystem = ecosystem,
                accent = accent,
                accentSoft = accentSoft,
                sectionSpacing = sectionSpacing,
                showWatchGoogleTv = showWatchGoogleTv,
                showWatchRoku = showWatchRoku,
                controlsEnabled = controlsEnabled,
                deviceDown = deviceDown,
                onScan = onScan,
                onRefreshStatus = onRefreshStatus,
                onOpenSetupGuide = onOpenSetupGuide,
                onSelect = onSelect,
                onGoogleSelect = onGoogleSelect,
                onWatchGoogleTv = onWatchGoogleTv,
                onWatchRoku = onWatchRoku,
                onDpad = onDpad,
                onCommand = onCommand,
                onVolume = onVolume,
                onLaunchPreset = onLaunchPreset,
                onLaunchPinnedApp = onLaunchPinnedApp,
                onSelectTab = onSelectTab,
                onTargetChange = onTargetChange,
                onLaunch = onLaunch,
                onSwitchInput = onSwitchInput,
                onSendText = onSendText,
                onKeyboardEnter = onKeyboardEnter,
                onKeyboardBackspace = onKeyboardBackspace,
                setupGuideBanner = setupGuideBanner
            )
        } else {
            StackedContent(
                viewportHeight = viewportHeight,
                sectionSpacing = sectionSpacing,
                state = state,
                ecosystem = ecosystem,
                accent = accent,
                accentSoft = accentSoft,
                showWatchGoogleTv = showWatchGoogleTv,
                showWatchRoku = showWatchRoku,
                controlsEnabled = controlsEnabled,
                deviceDown = deviceDown,
                onScan = onScan,
                onRefreshStatus = onRefreshStatus,
                onOpenSetupGuide = onOpenSetupGuide,
                onSelect = onSelect,
                onGoogleSelect = onGoogleSelect,
                onWatchGoogleTv = onWatchGoogleTv,
                onWatchRoku = onWatchRoku,
                onDpad = onDpad,
                onCommand = onCommand,
                onVolume = onVolume,
                onLaunchPreset = onLaunchPreset,
                onLaunchPinnedApp = onLaunchPinnedApp,
                onSelectTab = onSelectTab,
                onTargetChange = onTargetChange,
                onLaunch = onLaunch,
                onSwitchInput = onSwitchInput,
                onSendText = onSendText,
                onKeyboardEnter = onKeyboardEnter,
                onKeyboardBackspace = onKeyboardBackspace,
                setupGuideBanner = setupGuideBanner
            )
        }
    }
}

@Composable
private fun TwoPaneContent(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    sectionSpacing: Dp,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    controlsEnabled: Boolean,
    deviceDown: Boolean,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onSelect: (String) -> Unit,
    onGoogleSelect: (com.rgremote.app.domain.RegisteredDevice) -> Unit,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onDpad: (DpadDirection) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (VolumeCommand) -> Unit,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (HdmiPort) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardEnter: () -> Unit,
    onKeyboardBackspace: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        RemoteSurface(
            modifier = Modifier
                .weight(0.56f, fill = true)
                .fillMaxSize(),
            state = state,
            ecosystem = ecosystem,
            accent = accent,
            accentSoft = accentSoft,
            enabled = controlsEnabled,
            powerEnabled = state.selectedDevice != null,
            rokuControls = state.selectedRokuControls,
            showWatchGoogleTv = showWatchGoogleTv,
            showWatchRoku = showWatchRoku,
            onSelect = onSelect,
            onGoogleSelect = onGoogleSelect,
            onWatchGoogleTv = onWatchGoogleTv,
            onWatchRoku = onWatchRoku,
            onDpad = onDpad,
            onCommand = onCommand,
            onVolume = onVolume,
            onScan = onScan,
            onRefreshStatus = onRefreshStatus,
            onOpenSetupGuide = onOpenSetupGuide,
        )
        Column(
            modifier = Modifier
                .weight(0.44f, fill = true)
                .fillMaxSize()
                .alpha(if (deviceDown) 0.35f else 1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            setupGuideBanner()
            RemoteShortcutDock(
                state = state,
                ecosystem = ecosystem,
                accent = accent,
                onLaunchPreset = onLaunchPreset,
                onLaunchPinnedApp = onLaunchPinnedApp,
                onSelectTab = onSelectTab
            )
            RemoteUtilitiesDock(
                state = state,
                ecosystem = ecosystem,
                accent = accent,
                target = state.launchTarget,
                onTargetChange = onTargetChange,
                onLaunch = onLaunch,
                onSwitchInput = onSwitchInput,
                onSendText = onSendText,
                onKeyboardEnter = onKeyboardEnter,
                onKeyboardBackspace = onKeyboardBackspace
            )
        }
    }
}

@Composable
private fun StackedContent(
    viewportHeight: Dp,
    sectionSpacing: Dp,
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    controlsEnabled: Boolean,
    deviceDown: Boolean,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onSelect: (String) -> Unit,
    onGoogleSelect: (com.rgremote.app.domain.RegisteredDevice) -> Unit,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onDpad: (DpadDirection) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (VolumeCommand) -> Unit,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (HdmiPort) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardEnter: () -> Unit,
    onKeyboardBackspace: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    val remoteWeight = if (setupGuideNeeded(state)) 0.64f else 0.72f
    val dockWeight = 1f - remoteWeight

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        setupGuideBanner()
        RemoteSurface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(remoteWeight, fill = true),
            state = state,
            ecosystem = ecosystem,
            accent = accent,
            accentSoft = accentSoft,
            enabled = controlsEnabled,
            powerEnabled = state.selectedDevice != null,
            rokuControls = state.selectedRokuControls,
            showWatchGoogleTv = showWatchGoogleTv,
            showWatchRoku = showWatchRoku,
            onSelect = onSelect,
            onGoogleSelect = onGoogleSelect,
            onWatchGoogleTv = onWatchGoogleTv,
            onWatchRoku = onWatchRoku,
            onDpad = onDpad,
            onCommand = onCommand,
            onVolume = onVolume,
            onScan = onScan,
            onRefreshStatus = onRefreshStatus,
            onOpenSetupGuide = onOpenSetupGuide,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(dockWeight, fill = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (deviceDown) 0.35f else 1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                RemoteShortcutDock(
                    state = state,
                    ecosystem = ecosystem,
                    accent = accent,
                    onLaunchPreset = onLaunchPreset,
                    onLaunchPinnedApp = onLaunchPinnedApp,
                    onSelectTab = onSelectTab
                )
                RemoteUtilitiesDock(
                    state = state,
                    ecosystem = ecosystem,
                    accent = accent,
                    target = state.launchTarget,
                    onTargetChange = onTargetChange,
                    onLaunch = onLaunch,
                    onSwitchInput = onSwitchInput,
                    onSendText = onSendText,
                    onKeyboardEnter = onKeyboardEnter,
                    onKeyboardBackspace = onKeyboardBackspace
                )
            }
            if (deviceDown) {
                OfflineOverlay(
                    modifier = Modifier.matchParentSize(),
                    state = state,
                    accent = accent,
                    onScan = onScan,
                    onRefreshStatus = onRefreshStatus
                )
            }
        }
    }
}

@Composable
private fun OfflineOverlay(
    modifier: Modifier = Modifier,
    state: RGRemoteUiState,
    accent: Color,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.selectedDevice?.let {
                        stringResource(
                            R.string.offline_device_status,
                            it.friendlyName,
                            connectionStatusText(state.connectionStatus).lowercase()
                        )
                    } ?: connectionStatusText(state.connectionStatus),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = stringResource(R.string.offline_hint),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    enabled = !state.isScanning,
                    onClick = onScan
                ) {
                    Text(stringResource(R.string.common_scan))
                }
                androidx.compose.material3.Button(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = NeoCard.copy(alpha = 0.9f),
                        contentColor = PrimaryText
                    ),
                    onClick = onRefreshStatus
                ) {
                    Text(stringResource(R.string.header_menu_refresh_status))
                }
            }
        }
    }
}
