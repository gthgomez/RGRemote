package com.rgremote.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val deviceDown = state.connectionStatus == ConnectionStatus.OFFLINE ||
            state.connectionStatus == ConnectionStatus.CONNECTION_FAILED
    val controlsEnabled = state.selectedDevice != null && !deviceDown

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
                    onSwitchInput = onSwitchInput
                )
            }
            if (deviceDown) {
                OfflineDockOverlay(
                    status = state.connectionStatus,
                    accent = accent,
                    accentSoft = accentSoft,
                    onScan = onScan,
                    onRetry = onRefreshStatus
                )
            }
        }
    }
}

/**
 * Overlay painted over the dimmed dock while the selected device is offline or
 * the connection failed. A full-area sibling behind the card still swallows every
 * touch so the dimmed controls cannot mis-fire, while the centered glass card
 * explains what happened and offers recovery actions.
 */
@Composable
private fun OfflineDockOverlay(
    status: ConnectionStatus,
    accent: Color,
    accentSoft: Color,
    onScan: () -> Unit,
    onRetry: () -> Unit
) {
    val connectionFailed = status == ConnectionStatus.CONNECTION_FAILED
    val borderColor = if (connectionFailed) DangerRed.copy(alpha = 0.55f) else GlassStroke
    val glowColor = if (connectionFailed) DangerRed else SecondaryText
    Box(modifier = Modifier.matchParentSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .widthIn(max = 320.dp)
                .neoGlow(glowColor, if (connectionFailed) 0.22f else 0.10f, radius = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = NeoCard.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (connectionFailed) "Connection failed" else "Device offline",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Check the TV is powered on and the phone is on the same Wi-Fi network.",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.78f)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        onClick = onScan
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Scan again",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onRetry),
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = accentSoft,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Retry",
                                color = PrimaryText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
