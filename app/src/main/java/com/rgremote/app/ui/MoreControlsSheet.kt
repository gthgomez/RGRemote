package com.rgremote.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rgremote.app.R
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.VolumeCommand

/**
 * Secondary controls surface for the halo screen: volume first (the most-used
 * hidden control), then the existing shortcut and utilities docks, and entry
 * points to Apps and Settings. Reuses the existing dock composables — no
 * duplicated behavior.
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.halo_more_controls),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    enabled = volumeEnabled,
                    onClick = { onVolume(VolumeCommand.UP) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.remote_volume_up)) }
                Button(
                    enabled = volumeEnabled,
                    onClick = { onVolume(VolumeCommand.MUTE) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.remote_mute)) }
                Button(
                    enabled = volumeEnabled,
                    onClick = { onVolume(VolumeCommand.DOWN) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.remote_volume_down)) }
            }
            if (showWatchGoogleTv || showWatchRoku) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showWatchGoogleTv) {
                        Button(
                            onClick = {
                                onWatchGoogleTv()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.ecosystem_google_tv)) }
                    }
                    if (showWatchRoku) {
                        Button(
                            onClick = {
                                onWatchRoku()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.ecosystem_roku)) }
                    }
                }
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSelectTab(RemoteTab.APPS) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentSoft,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.nav_apps)) }
                Button(
                    onClick = { onSelectTab(RemoteTab.SETTINGS) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentSoft,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.nav_settings)) }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
