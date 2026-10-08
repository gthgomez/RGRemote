package com.rgremote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgremote.app.R
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.ui.theme.HaloSpec
import com.rgremote.app.ui.theme.rememberRingBrush
import com.rgremote.app.ui.theme.LocalHaloColors

/**
 * Secondary controls surface for the halo screen. Styled with the halo glass
 * language rather than default Material buttons: circular icon wells for
 * volume, the existing shortcut/utilities docks, and glass pills for Apps and
 * Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreControlsSheet(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    accentSoft: Color,
    volumeEnabled: Boolean,
    onVolume: (VolumeCommand) -> Unit,
    showWatchGoogleTv: Boolean,
    showWatchRoku: Boolean,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (HdmiPort) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardEnter: () -> Unit,
    onKeyboardBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val halo = LocalHaloColors.current
    val ringBrush = rememberRingBrush()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HaloSpec.ScreenHorizontalPaddingDp.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.halo_more_controls),
                style = MaterialTheme.typography.titleMedium.copy(brush = ringBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
            // Volume first: three circular wells in one capsule row.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (volumeEnabled) 1f else 0.4f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SheetWell(
                    enabled = volumeEnabled,
                    contentDescription = stringResource(R.string.remote_volume_up),
                    onClick = { onVolume(VolumeCommand.UP) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = halo.dockIcon
                    )
                }
                SheetSeparator()
                SheetWell(
                    enabled = volumeEnabled,
                    contentDescription = stringResource(R.string.remote_mute),
                    onClick = { onVolume(VolumeCommand.MUTE) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = null,
                        tint = halo.dockIcon
                    )
                }
                SheetSeparator()
                SheetWell(
                    enabled = volumeEnabled,
                    contentDescription = stringResource(R.string.remote_volume_down),
                    onClick = { onVolume(VolumeCommand.DOWN) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = null,
                        tint = halo.dockIcon
                    )
                }
            }
            if (showWatchGoogleTv || showWatchRoku) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showWatchGoogleTv) {
                        GlassPill(
                            label = stringResource(R.string.ecosystem_google_tv),
                            onClick = {
                                onWatchGoogleTv()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showWatchRoku) {
                        GlassPill(
                            label = stringResource(R.string.ecosystem_roku),
                            onClick = {
                                onWatchRoku()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            RemoteShortcutDock(
                state = state,
                ecosystem = ecosystem,
                accent = accent,
                onLaunchPreset = onLaunchPreset,
                onLaunchPinnedApp = onLaunchPinnedApp,
                onSelectTab = onSelectTab,
                // The sheet already has an Apps pill; no duplicate link here.
                showAppsLink = false
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassPill(
                    label = stringResource(R.string.nav_apps),
                    onClick = { onSelectTab(RemoteTab.APPS) },
                    modifier = Modifier.weight(1f)
                )
                GlassPill(
                    label = stringResource(R.string.nav_settings),
                    onClick = { onSelectTab(RemoteTab.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SheetWell(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val halo = LocalHaloColors.current
    Box(
        modifier = Modifier
            .size(HaloSpec.SheetWellDp.dp)
            .background(halo.dockWell, CircleShape)
            .border(1.dp, halo.dockBorder, CircleShape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SheetSeparator() {
    Box(
        Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(28.dp)
            .background(LocalHaloColors.current.dockSeparator)
    )
}

/** Halo-styled text pill: glass fill, ring-gradient border and label. */
@Composable
private fun GlassPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val halo = LocalHaloColors.current
    val ringBrush = rememberRingBrush()
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .background(halo.dockFill, shape)
            .border(1.dp, ringBrush, shape)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(brush = ringBrush),
            fontWeight = FontWeight.Medium
        )
    }
}
