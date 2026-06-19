package com.rgremote.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand

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
    setupGuideBanner: @Composable () -> Unit,
) {
    val remoteWeight = if (setupGuideNeeded(state)) 0.64f else 0.72f
    val dockWeight = 1f - remoteWeight

    Column(
        modifier = modifier.fillMaxSize(),
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
            enabled = state.selectedDevice != null,
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
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(dockWeight, fill = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    onSwitchInput = onSwitchInput
                )
            }
        }
    }
}
