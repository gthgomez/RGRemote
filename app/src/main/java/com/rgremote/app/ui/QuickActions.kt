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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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

private val RokuDefaultChannelPresets = listOf(
    "Netflix" to "12",
    "YouTube" to "837",
    "Disney+" to "291097",
)

private val GoogleTvDefaultAppPresets = listOf(
    "YouTube" to "com.google.android.youtube.tv",
    "Netflix" to "com.netflix.ninja",
    "Disney+" to "com.disney.disneyplus",
)

/** Slim shortcut row embedded inside the hero remote card. */
@Composable
internal fun RemoteShortcutDock(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    onLaunchPreset: (String) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
) {
    val ecosystemPins = state.pinnedApps.filter { it.deviceType == ecosystem.type }.take(4)
    val fallbackPresets = when (ecosystem.type) {
        DeviceType.ROKU_TV -> RokuDefaultChannelPresets
        DeviceType.GOOGLE_TV -> GoogleTvDefaultAppPresets
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shortcuts",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SecondaryText
            )
            Text(
                text = "Apps",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectTab(RemoteTab.APPS) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        PinnedQuickActionsRow(
            pins = ecosystemPins,
            accent = accent,
            fallbackPresets = fallbackPresets,
            onLaunchPinned = onLaunchPinnedApp,
            onLaunchPreset = onLaunchPreset,
            onAddApp = { onSelectTab(RemoteTab.APPS) }
        )
    }
}

/** Collapsed utilities below the hero remote (HDMI, launch). */
@Composable
internal fun RemoteUtilitiesDock(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    target: String,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onSwitchInput: (HdmiPort) -> Unit,
) {
    if (!state.showUtilitiesDock) return
    val setupIncomplete = state.setupIncomplete
    var utilitiesExpanded by rememberSaveable(setupIncomplete) {
        mutableStateOf(setupIncomplete)
    }
    UtilitiesExpander(
        expanded = utilitiesExpanded,
        accent = accent,
        onToggle = { utilitiesExpanded = !utilitiesExpanded }
    ) {
        if (ecosystem.type == DeviceType.ROKU_TV) {
            ActionSectionLabel("HDMI INPUTS")
            InputSwitchPanel(
                state = state,
                accent = accent,
                onSwitchInput = onSwitchInput
            )
        }
        ActionSectionLabel(if (ecosystem.type == DeviceType.GOOGLE_TV) "LAUNCH APP" else "LAUNCH")
        LaunchPanel(
            target = target,
            accent = accent,
            launchLabel = ecosystem.launchLabel,
            placeholder = if (ecosystem.type == DeviceType.GOOGLE_TV) {
                "Package name or deep link"
            } else {
                "Channel ID or deep link"
            },
            onTargetChange = onTargetChange,
            onLaunch = onLaunch
        )
    }
}

@Composable
private fun UtilitiesExpander(
    expanded: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = NeoCard.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, GlassStroke.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Utilities",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse utilities" else "Expand utilities",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
internal fun ActionSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryText,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private sealed interface QuickActionSlot {
    data class Pinned(val app: AppLaunchTarget) : QuickActionSlot
    data class Preset(val label: String, val target: String) : QuickActionSlot
    data object Add : QuickActionSlot
}

internal fun watchModeVisibility(state: RGRemoteUiState): Pair<Boolean, Boolean> {
    val google = state.googleTvDevice
    val googleReady = state.rokuDevice != null &&
        google != null &&
        state.isGoogleTvPaired &&
        google.hdmiPortMapping != null
    val rokuReady = state.rokuDevice != null
    val selectedType = state.selectedDevice?.type
    val showWatchGoogleTv = googleReady && selectedType != DeviceType.GOOGLE_TV
    val showWatchRoku = rokuReady && selectedType != DeviceType.ROKU_TV
    return showWatchGoogleTv to showWatchRoku
}

@Composable
internal fun PinnedQuickActionsRow(
    pins: List<AppLaunchTarget>,
    accent: Color,
    fallbackPresets: List<Pair<String, String>>,
    onLaunchPinned: (AppLaunchTarget) -> Unit,
    onLaunchPreset: (String) -> Unit,
    onAddApp: () -> Unit,
) {
    val slots = buildList {
        pins.forEach { add(QuickActionSlot.Pinned(it)) }
        if (isEmpty() && fallbackPresets.isNotEmpty()) {
            fallbackPresets.forEach { (label, target) -> add(QuickActionSlot.Preset(label, target)) }
        }
        while (size < 4) {
            add(QuickActionSlot.Add)
        }
    }.take(4)

    if (pins.isEmpty() && fallbackPresets.isEmpty()) {
        Text(
            text = "Pin apps in Apps tab",
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slots.forEach { slot ->
            when (slot) {
                is QuickActionSlot.Pinned -> {
                    val app = slot.app
                    QuickActionChip(
                        modifier = Modifier.weight(1f),
                        label = app.displayName,
                        isAdd = false,
                        accent = accent,
                        enabled = true,
                        onClick = { onLaunchPinned(app) }
                    )
                }
                is QuickActionSlot.Preset -> {
                    QuickActionChip(
                        modifier = Modifier.weight(1f),
                        label = slot.label,
                        isAdd = false,
                        accent = accent,
                        enabled = slot.target.isNotBlank(),
                        onClick = { onLaunchPreset(slot.target) }
                    )
                }
                QuickActionSlot.Add -> {
                    QuickActionChip(
                        modifier = Modifier.weight(1f),
                        label = "+ Add",
                        isAdd = true,
                        accent = accent,
                        enabled = true,
                        onClick = onAddApp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    modifier: Modifier,
    label: String,
    isAdd: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isAdd) accent.copy(alpha = 0.24f) else NeoCard.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, if (isAdd) accent.copy(alpha = 0.45f) else GlassStroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            PresetLogo(label = label, accent = accent)
        }
    }
}

@Composable
internal fun PresetLaunchRow(
    accent: Color,
    presets: List<Pair<String, String>>,
    onLaunchPreset: (String) -> Unit,
    onAddClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.take(4).forEach { (label, target) ->
            val isAdd = label.startsWith("+")
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isAdd || target.isNotBlank()) {
                        if (isAdd) onAddClick() else onLaunchPreset(target)
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isAdd) accent.copy(alpha = 0.24f) else NeoCard.copy(alpha = 0.76f),
                border = BorderStroke(1.dp, if (isAdd) accent.copy(alpha = 0.45f) else GlassStroke)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PresetLogo(label = label, accent = accent)
                }
            }
        }
    }
}

@Composable
internal fun LaunchPanel(
    target: String,
    accent: Color,
    launchLabel: String,
    placeholder: String,
    onTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 350.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LaunchInput(target, placeholder, onTargetChange)
                LaunchButton(
                    modifier = Modifier.fillMaxWidth(),
                    accent = accent,
                    launchLabel = launchLabel,
                    onLaunch = onLaunch
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LaunchInput(
                    modifier = Modifier.weight(1f),
                    target = target,
                    placeholder = placeholder,
                    onTargetChange = onTargetChange
                )
                LaunchButton(
                    modifier = Modifier.width(150.dp),
                    accent = accent,
                    launchLabel = launchLabel,
                    onLaunch = onLaunch
                )
            }
        }
    }
}

@Composable
private fun PresetLogo(label: String, accent: Color) {
    val isAdd = label.startsWith("+")
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAdd) {
            Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add", color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            return
        }

        when (label) {
            "Netflix" -> Text(
                text = "NETFLIX",
                color = Color(0xFFFF3045),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            "YouTube" -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 20.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0B101C), modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("YouTube", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            "Disney+" -> Text(
                text = "Disney+",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            else -> Text(label, color = PrimaryText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LaunchInput(
    target: String,
    placeholder: String,
    onTargetChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        modifier = modifier.height(54.dp),
        value = target,
        onValueChange = onTargetChange,
        singleLine = true,
        label = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText)
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LaunchButton(
    modifier: Modifier,
    accent: Color,
    launchLabel: String,
    onLaunch: () -> Unit
) {
    Button(
        modifier = modifier.height(54.dp).neoGlow(accent, 0.18f, radius = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 12.dp),
        onClick = onLaunch
    ) {
        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(launchLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun InputSwitchPanel(
    state: RGRemoteUiState,
    accent: Color,
    onSwitchInput: (HdmiPort) -> Unit
) {
    if (state.rokuDevice == null) return
    val currentInput = state.activeApp?.inferredHdmiPort
    val ports = listOf(HdmiPort.HDMI1, HdmiPort.HDMI2, HdmiPort.HDMI3, HdmiPort.HDMI4)
    val fontScale = LocalDensity.current.fontScale
    val enabled = state.selectedRokuControls.supportsInputSwitching != false
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumnGrid = fontScale >= 1.2f || maxWidth < 315.dp

        if (useTwoColumnGrid) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ports.chunked(2).forEach { rowPorts ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowPorts.forEach { port ->
                            HdmiInputButton(
                                modifier = Modifier.weight(1f),
                                port = port,
                                selected = currentInput == port,
                                accent = accent,
                                enabled = enabled,
                                onSwitchInput = onSwitchInput
                            )
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ports.forEach { port ->
                    HdmiInputButton(
                        modifier = Modifier.weight(1f),
                        port = port,
                        selected = currentInput == port,
                        accent = accent,
                        enabled = enabled,
                        onSwitchInput = onSwitchInput
                    )
                }
            }
        }
    }
}

@Composable
private fun HdmiInputButton(
    modifier: Modifier,
    port: HdmiPort,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onSwitchInput: (HdmiPort) -> Unit
) {
    Button(
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent.copy(alpha = 0.88f) else NeoCard.copy(alpha = 0.72f),
            contentColor = if (selected) Color.White else SecondaryText
        ),
        border = BorderStroke(1.dp, if (selected) accent else GlassStroke),
        contentPadding = PaddingValues(horizontal = 6.dp),
        onClick = { onSwitchInput(port) }
    ) {
        Icon(Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = port.displayName.replace(" ", ""),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
