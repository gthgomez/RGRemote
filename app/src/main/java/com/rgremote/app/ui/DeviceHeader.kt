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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

@Composable
internal fun ActiveDeviceHeader(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onSelect: (String) -> Unit,
    onGoogleSelect: (RegisteredDevice) -> Unit,
    compact: Boolean = true,
) {
    val selected = state.selectedDevice
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        color = NeoSurface.copy(alpha = if (compact) 0.72f else 0.78f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.12f), NeoSurface.copy(alpha = 0.82f), Color(0xFF0D1220))
                    )
                )
                .cyberEtch(accent, alpha = if (ecosystem.type == DeviceType.ROKU_TV) 0.12f else 0.06f)
                .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EcosystemBadge(
                        deviceType = selected?.type ?: ecosystem.type,
                        primary = ecosystem.primary,
                        accent = ecosystem.accent,
                        size = if (compact) 34.dp else 42.dp
                    )
                    var deviceMenuExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        if (!compact) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "RG",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = accent
                                )
                                Text(
                                    text = "Remote",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryText
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { deviceMenuExpanded = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = selected?.friendlyName ?: "No device",
                                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch device",
                                tint = PrimaryText.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            DropdownMenu(
                                expanded = deviceMenuExpanded,
                                onDismissRequest = { deviceMenuExpanded = false }
                            ) {
                                state.devices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(device.friendlyName) },
                                        onClick = {
                                            deviceMenuExpanded = false
                                            onSelect(device.id)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (device.type == DeviceType.ROKU_TV) Icons.Default.Tv else Icons.Default.SmartDisplay,
                                                contentDescription = null,
                                                tint = if (device.id == selected?.id) accent else SecondaryText
                                            )
                                        }
                                    )
                                }
                                if (state.devices.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No devices found") },
                                        onClick = { deviceMenuExpanded = false },
                                        enabled = false
                                    )
                                }
                            }
                        }
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
                }
                Box {
                    RoundIconButton(
                        icon = Icons.Default.MoreVert,
                        label = "More actions",
                        accent = accent,
                        enabled = true,
                        size = if (compact) 38.dp else 42.dp,
                        onClick = { menuExpanded = true }
                    )
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
            if (useEcosystemSegmentBar(state)) {
                EcosystemSegmentBar(
                    state = state,
                    selected = selected,
                    onSelect = onSelect,
                    onGoogleSelect = onGoogleSelect
                )
            } else {
                DeviceSwitcherGrid(
                    state = state,
                    selected = selected,
                    onSelect = onSelect,
                    onGoogleSelect = onGoogleSelect
                )
            }
        }
    }
}

private fun useEcosystemSegmentBar(state: RGRemoteUiState): Boolean {
    val devices = state.devices
    val rokuCount = devices.count { it.type == DeviceType.ROKU_TV }
    val googleCount = devices.count { it.type == DeviceType.GOOGLE_TV }
    if (devices.isEmpty()) return true
    // When multiple devices of the same type exist, use the grid so each device
    // gets its own selectable chip. Otherwise the segment bar hides the extras.
    return rokuCount <= 1 && googleCount <= 1
}

@Composable
internal fun HeaderOverflowMenu(
    expanded: Boolean,
    state: RGRemoteUiState,
    selected: RegisteredDevice?,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenSetupGuide: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (state.isScanning) "Scanning..." else "Scan devices") },
            onClick = {
                onDismiss()
                if (!state.isScanning) onScan()
            },
            enabled = !state.isScanning,
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Refresh status") },
            onClick = {
                onDismiss()
                onRefreshStatus()
            },
            enabled = selected != null,
            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Connection guide") },
            onClick = {
                onDismiss()
                onOpenSetupGuide()
            },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) }
        )
    }
}

@Composable
internal fun ActiveTvSubtitle(
    state: RGRemoteUiState,
    accent: Color,
    onOpenRemote: () -> Unit,
) {
    val device = state.selectedDevice
    val text = when {
        device != null -> {
            val name = device.friendlyName.trim()
            if (name.isNotEmpty()) "Controlling: $name" else "Controlling: ${if (device.type == DeviceType.ROKU_TV) "Roku" else "Google TV"}"
        }
        else -> "No device selected — open Remote"
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenRemote)
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Two-pill switcher (Roku vs Google TV) — no pairing/HDMI detail lines. */
@Composable
internal fun EcosystemSegmentBar(
    state: RGRemoteUiState,
    selected: RegisteredDevice?,
    onSelect: (String) -> Unit,
    onGoogleSelect: (RegisteredDevice) -> Unit,
) {
    val rokus = state.rokuDevices.ifEmpty { listOfNotNull(state.rokuDevice) }
    val googles = state.googleDevices.ifEmpty { listOfNotNull(state.googleTvDevice) }
    if (rokus.isEmpty() && googles.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeoCard.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scan to add a TV",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (rokus.isNotEmpty()) {
            SegmentGroup(
                modifier = Modifier.weight(1f),
                label = "Roku",
                devices = rokus,
                selectedId = selected?.id,
                accent = RokuPrimary,
                showAttentionDot = false,
                onSelectDevice = { device -> onSelect(device.id) }
            )
        }
        if (googles.isNotEmpty()) {
            val primaryGoogle = googles.first()
            val needsAttention =
                !state.pairedDeviceIds.contains(primaryGoogle.id) || primaryGoogle.hdmiPortMapping == null
            SegmentGroup(
                modifier = Modifier.weight(1f),
                label = "Google TV",
                devices = googles,
                selectedId = selected?.id,
                accent = GooglePrimary,
                showAttentionDot = needsAttention && selected?.id != primaryGoogle.id,
                onSelectDevice = onGoogleSelect
            )
        }
    }
}

@Composable
private fun SegmentGroup(
    modifier: Modifier,
    label: String,
    devices: List<RegisteredDevice>,
    selectedId: String?,
    accent: Color,
    showAttentionDot: Boolean,
    onSelectDevice: (RegisteredDevice) -> Unit,
) {
    val primary = devices.first()
    var overflowExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EcosystemSegment(
            modifier = Modifier.weight(1f),
            label = label,
            selected = selectedId == primary.id,
            accent = accent,
            enabled = true,
            showAttentionDot = showAttentionDot,
            onClick = { onSelectDevice(primary) }
        )
        if (devices.size > 1) {
            Box {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { overflowExpanded = true },
                    shape = CircleShape,
                    color = NeoCard.copy(alpha = 0.48f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "More $label devices",
                            tint = accent.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = devices.size.toString(),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 8.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = device.friendlyName,
                                    color = if (device.id == selectedId) accent else PrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onSelectDevice(device)
                            },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (device.isOnline) SuccessGreen else SecondaryText.copy(alpha = 0.38f)
                                        )
                                )
                            },
                            trailingIcon = if (device.id == selectedId) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EcosystemSegment(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    showAttentionDot: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.28f) else NeoCard.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.72f else 0.14f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = if (selected) Color.White else SecondaryText.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showAttentionDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 10.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.9f))
                )
            }
        }
    }
}

@Composable
private fun DeviceSwitcherGrid(
    state: RGRemoteUiState,
    selected: RegisteredDevice?,
    onSelect: (String) -> Unit,
    onGoogleSelect: (RegisteredDevice) -> Unit
) {
    val devices = state.devices
    if (devices.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDeviceChip(
                modifier = Modifier.weight(1f),
                label = "Roku TV",
                detail = "Scan",
                deviceType = DeviceType.ROKU_TV,
                selected = false,
                enabled = false,
                accent = RokuPrimary,
                onClick = {}
            )
            CompactDeviceChip(
                modifier = Modifier.weight(1f),
                label = "Google TV",
                detail = "Scan",
                deviceType = DeviceType.GOOGLE_TV,
                selected = false,
                enabled = false,
                accent = GooglePrimary,
                onClick = {}
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.chunked(2).forEach { rowDevices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowDevices.forEach { device ->
                    val accent = if (device.type == DeviceType.ROKU_TV) RokuPrimary else GooglePrimary
                    CompactDeviceChip(
                        modifier = Modifier.weight(1f),
                        label = device.friendlyName,
                        detail = device.switcherDetail(state),
                        deviceType = device.type,
                        selected = selected?.id == device.id,
                        enabled = true,
                        accent = accent,
                        onClick = {
                            if (device.type == DeviceType.GOOGLE_TV) {
                                onGoogleSelect(device)
                            } else {
                                onSelect(device.id)
                            }
                        }
                    )
                }
                if (rowDevices.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun RegisteredDevice.switcherDetail(state: RGRemoteUiState): String =
    when (type) {
        DeviceType.ROKU_TV -> if (isOnline) "Roku / Online" else "Roku / Offline"
        DeviceType.GOOGLE_TV -> {
            val paired = if (state.pairedDeviceIds.contains(id)) "Paired" else "Pair"
            val mapped = hdmiPortMapping?.displayName ?: "Map HDMI"
            "Google TV / $paired / $mapped"
        }
    }

@Composable
internal fun CompactDeviceChip(
    modifier: Modifier,
    label: String,
    detail: String,
    deviceType: DeviceType,
    selected: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.22f) else NeoCard.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.76f else 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected && enabled) accent else SecondaryText.copy(alpha = 0.38f))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) PrimaryText else SecondaryText.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accent else SecondaryText.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
            } else if (deviceType == DeviceType.GOOGLE_TV) {
                GoogleTvGlyph()
            } else {
                Text("R", color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun DeviceTiles(
    state: RGRemoteUiState,
    accent: Color,
    onSelect: (String) -> Unit,
    onGoogleTileClick: (RegisteredDevice) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EcosystemDeviceCard(
            modifier = Modifier.weight(1f),
            fallbackTitle = "TCL Roku TV",
            platformLabel = "Roku TV",
            device = state.rokuDevice,
            selected = state.selectedDevice?.type == DeviceType.ROKU_TV,
            detail = state.rokuDevice?.let { "${it.ipAddress}:${it.port}" } ?: "Scan on Wi-Fi",
            primary = RokuPrimary,
            accent = RokuAccent,
            onClick = { device -> onSelect(device.id) }
        )
        EcosystemDeviceCard(
            modifier = Modifier.weight(1f),
            fallbackTitle = "Onn Google TV",
            platformLabel = "Google TV",
            device = state.googleTvDevice,
            selected = state.selectedDevice?.type == DeviceType.GOOGLE_TV,
            detail = state.googleTvDevice?.let { google ->
                val mapped = google.hdmiPortMapping?.displayName ?: "Map HDMI"
                val paired = if (state.pairedDeviceIds.contains(google.id)) "Paired" else "Tap to pair"
                "$paired / $mapped"
            } ?: "Scan on Wi-Fi",
            primary = GooglePrimary,
            accent = GoogleAccent,
            onClick = onGoogleTileClick
        )
    }
    EcosystemSignalBar(accent = accent)
}

@Composable
internal fun EcosystemDeviceCard(
    modifier: Modifier,
    fallbackTitle: String,
    platformLabel: String,
    device: RegisteredDevice?,
    selected: Boolean,
    detail: String,
    primary: Color,
    accent: Color,
    onClick: (RegisteredDevice) -> Unit
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "deviceCardGlow"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else Color(0xFF303744),
        animationSpec = tween(durationMillis = 300),
        label = "deviceCardBorder"
    )
    Surface(
        modifier = modifier
            .height(148.dp)
            .neoGlow(primary, glowAlpha * 0.36f, radius = 22.dp)
            .clickable(enabled = device != null) { device?.let(onClick) },
        shape = RoundedCornerShape(22.dp),
        color = if (selected) NeoCard.copy(alpha = 0.96f) else NeoSurface.copy(alpha = 0.78f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor.copy(alpha = if (selected) 0.95f else 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EcosystemBadge(
                    deviceType = device?.type ?: if (platformLabel.startsWith("Roku")) DeviceType.ROKU_TV else DeviceType.GOOGLE_TV,
                    primary = primary,
                    accent = accent
                )
                AnimatedVisibility(selected) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                            .neoGlow(SuccessGreen, 0.5f, radius = 10.dp)
                    )
                }
            }
            Text(
                text = device?.friendlyName ?: fallbackTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = platformLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else SecondaryText.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) PrimaryText.copy(alpha = 0.86f) else SecondaryText.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun EcosystemBadge(
    deviceType: DeviceType,
    primary: Color,
    accent: Color,
    size: Dp = 38.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.98f), accent.copy(alpha = 0.60f), Color(0xFF4B239C))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        if (deviceType == DeviceType.ROKU_TV) {
            Text(
                "R",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = if (size > 48.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge
            )
        } else {
            GoogleTvGlyph()
        }
    }
}

@Composable
internal fun GoogleTvGlyph() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniDot(Color(0xFF4285F4))
            MiniDot(Color(0xFFEA4335))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniDot(Color(0xFFFBBC04))
            MiniDot(Color(0xFF34A853))
        }
    }
}

@Composable
internal fun MiniDot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
internal fun EcosystemSignalBar(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.08f)))
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .width(72.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(accent)
                .neoGlow(accent, 0.45f, radius = 8.dp)
        )
        Box(Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.08f)))
    }
}
