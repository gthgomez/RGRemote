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
    onBeginDpadSequence: () -> Long,
    onDpadRepeat: (DpadDirection, Long) -> Unit,
    onEndDpadSequence: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    val deviceDown = state.connectionStatus == ConnectionStatus.OFFLINE ||
            state.connectionStatus == ConnectionStatus.CONNECTION_FAILED
    // CHECKING and NOT_PAIRED must never look ready (docs/ui-target.md chip states).
    val controlsUnavailable = state.connectionStatus == ConnectionStatus.CHECKING ||
            state.connectionStatus == ConnectionStatus.NOT_PAIRED
    val controlsEnabled = state.selectedDevice != null && !deviceDown && !controlsUnavailable

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
                onBeginDpadSequence = onBeginDpadSequence,
                onDpadRepeat = onDpadRepeat,
                onEndDpadSequence = onEndDpadSequence,
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
                onBeginDpadSequence = onBeginDpadSequence,
                onDpadRepeat = onDpadRepeat,
                onEndDpadSequence = onEndDpadSequence,
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
    onBeginDpadSequence: () -> Long,
    onDpadRepeat: (DpadDirection, Long) -> Unit,
    onEndDpadSequence: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    val volumeEnabled = controlsEnabled &&
            (ecosystem.type != com.rgremote.app.domain.DeviceType.ROKU_TV ||
                state.selectedRokuControls.supportsVolume != false)
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        HaloRemoteScreen(
            modifier = Modifier
                .weight(0.56f, fill = true)
                .fillMaxSize(),
            state = state,
            ecosystem = ecosystem,
            accent = accent,
            accentSoft = accentSoft,
            controlsEnabled = controlsEnabled,
            volumeEnabled = volumeEnabled,
            onScan = onScan,
            onRefreshStatus = onRefreshStatus,
            onOpenSetupGuide = onOpenSetupGuide,
            onSelect = onSelect,
            onGoogleSelect = onGoogleSelect,
            onBeginDpadSequence = onBeginDpadSequence,
            onDpadRepeat = onDpadRepeat,
            onEndDpadSequence = onEndDpadSequence,
            onSelectCommand = { onCommand(RemoteCommand.Select) },
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
            onKeyboardBackspace = onKeyboardBackspace
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
    onBeginDpadSequence: () -> Long,
    onDpadRepeat: (DpadDirection, Long) -> Unit,
    onEndDpadSequence: () -> Unit,
    setupGuideBanner: @Composable () -> Unit,
) {
    val volumeEnabled = controlsEnabled &&
            (ecosystem.type != com.rgremote.app.domain.DeviceType.ROKU_TV ||
                state.selectedRokuControls.supportsVolume != false)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        setupGuideBanner()
        HaloRemoteScreen(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            state = state,
            ecosystem = ecosystem,
            accent = accent,
            accentSoft = accentSoft,
            controlsEnabled = controlsEnabled,
            volumeEnabled = volumeEnabled,
            onScan = onScan,
            onRefreshStatus = onRefreshStatus,
            onOpenSetupGuide = onOpenSetupGuide,
            onSelect = onSelect,
            onGoogleSelect = onGoogleSelect,
            onBeginDpadSequence = onBeginDpadSequence,
            onDpadRepeat = onDpadRepeat,
            onEndDpadSequence = onEndDpadSequence,
            onSelectCommand = { onCommand(RemoteCommand.Select) },
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
            onKeyboardBackspace = onKeyboardBackspace
        )
    }
}
