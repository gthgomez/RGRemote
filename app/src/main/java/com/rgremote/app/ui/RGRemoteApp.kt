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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rgremote.app.R
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.roku.RokuPowerMode
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.duplicateDeviceCount
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.VolumeCommand

@Composable
fun RGRemoteApp(viewModel: RGRemoteViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RGRemoteScreen(
        state = state,
        onScan = viewModel::scan,
        onSelect = viewModel::selectDevice,
        onDpad = viewModel::sendDpad,
        onCommand = viewModel::send,
        onVolume = viewModel::sendVolume,
        onLaunchTargetChange = viewModel::updateLaunchTarget,
        onLaunch = viewModel::launchTypedTarget,
        onPairingPinChange = viewModel::updatePairingPin,
        onStartPairing = viewModel::startPairing,
        onFinishPairing = viewModel::finishPairing,
        onSetMapping = viewModel::setHdmiMapping,
        onSwitchTvInput = viewModel::switchTvInput,
        onRefreshStatus = viewModel::refreshStatus,
        onSelectTab = viewModel::selectTab,
        onRefreshRokuChannels = viewModel::refreshRokuChannels,
        onPinRokuChannel = viewModel::pinRokuChannel,
        onSaveGoogleTvApp = viewModel::saveGoogleTvApp,
        onRemovePinnedApp = viewModel::removePinnedApp,
        onLaunchPinnedApp = viewModel::launchPinnedApp,
        onResetAppPins = viewModel::resetAppPins,
        onAddManualDevice = viewModel::addManualDevice,
        onSelectGoogleDevice = viewModel::selectGoogleDevice,
        onWatchGoogleTv = viewModel::watchGoogleTv,
        onWatchRoku = viewModel::watchRoku,
        onClearUserFeedback = viewModel::clearUserFeedback,
        onHideConnectionGuide = { viewModel.setShowConnectionGuide(false) },
        onSetShowConnectionGuide = viewModel::setShowConnectionGuide,
        onSetShowUtilitiesDock = viewModel::setShowUtilitiesDock,
        onSetStartupScreen = viewModel::setStartupScreen,
        onRemoveSavedDevice = viewModel::removeSavedDevice,
        onDedupeSavedDevices = viewModel::dedupeSavedDevices
    )
}

@Composable
private fun RGRemoteScreen(
    state: RGRemoteUiState,
    onScan: () -> Unit,
    onSelect: (String) -> Unit,
    onDpad: (DpadDirection) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onVolume: (VolumeCommand) -> Unit,
    onLaunchTargetChange: (String) -> Unit,
    onLaunch: () -> Unit,
    onPairingPinChange: (String) -> Unit,
    onStartPairing: (RegisteredDevice) -> Unit,
    onFinishPairing: (RegisteredDevice) -> Unit,
    onSetMapping: (RegisteredDevice, HdmiPort?) -> Unit,
    onSwitchTvInput: (HdmiPort) -> Unit,
    onRefreshStatus: () -> Unit,
    onSelectTab: (RemoteTab) -> Unit,
    onRefreshRokuChannels: () -> Unit,
    onPinRokuChannel: (RokuApp) -> Unit,
    onSaveGoogleTvApp: (String, String) -> Unit,
    onRemovePinnedApp: (AppLaunchTarget) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onResetAppPins: () -> Unit,
    onAddManualDevice: (DeviceType, String, String) -> Unit,
    onSelectGoogleDevice: (String) -> Unit,
    onWatchGoogleTv: () -> Unit,
    onWatchRoku: () -> Unit,
    onClearUserFeedback: () -> Unit,
    onHideConnectionGuide: () -> Unit,
    onSetShowConnectionGuide: (Boolean) -> Unit,
    onSetShowUtilitiesDock: (Boolean) -> Unit,
    onSetStartupScreen: (RemoteTab?) -> Unit,
    onRemoveSavedDevice: (String) -> Unit,
    onDedupeSavedDevices: () -> Unit,
) {
    var showGoogleTvSetupDialog by rememberSaveable { mutableStateOf(false) }
    var editingGoogleTvApp by remember { mutableStateOf<AppLaunchTarget?>(null) }
    var showGoogleTvAppDialog by rememberSaveable { mutableStateOf(false) }
    var showManualDeviceDialog by rememberSaveable { mutableStateOf(false) }
    var showSetupGuideDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) }
    var manualAddAttempt by remember { mutableStateOf<ManualAddAttempt?>(null) }
    var manualSawProbe by remember { mutableStateOf(false) }
    var manualInlineMessage by remember { mutableStateOf<String?>(null) }
    var manualInlineIsError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val manualProbeProgressFallback = stringResource(R.string.manual_probe_progress_fallback)
    val manualAddFailedFallback = stringResource(R.string.manual_add_failed_fallback)
    val googleTv = state.googleTvDevice
    val ecosystem = rememberEcosystemStyle(state.selectedDevice?.type)
    val accent by animateColorAsState(
        targetValue = ecosystem.primary,
        animationSpec = tween(durationMillis = 320),
        label = "ecosystemAccent"
    )
    val accentSoft by animateColorAsState(
        targetValue = ecosystem.accent,
        animationSpec = tween(durationMillis = 320),
        label = "ecosystemAccentSoft"
    )

    LaunchedEffect(state.userFeedback) {
        val message = state.userFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearUserFeedback()
    }

    LaunchedEffect(showManualDeviceDialog) {
        if (!showManualDeviceDialog) return@LaunchedEffect
        manualAddAttempt = null
        manualSawProbe = false
        manualInlineMessage = null
        manualInlineIsError = false
    }

    LaunchedEffect(
        manualAddAttempt,
        state.isProbingManualDevice,
        state.userFeedback,
        state.connectionStatus,
        state.devices.size
    ) {
        val attempt = manualAddAttempt
        if (attempt == null) {
            return@LaunchedEffect
        }
        val probing = state.isProbingManualDevice
        when {
            probing -> {
                manualSawProbe = true
                manualInlineMessage = state.manualProbeMessage ?: manualProbeProgressFallback
                manualInlineIsError = false
            }
            state.userFeedback != null && state.userFeedback != attempt.priorFeedback -> {
                manualInlineMessage = state.userFeedback
                manualInlineIsError = true
                manualAddAttempt = null
            }
            state.devices.size > attempt.deviceCount ||
                (!attempt.priorOnline && state.connectionStatus == ConnectionStatus.ONLINE) -> {
                manualInlineMessage = null
                manualAddAttempt = null
                showManualDeviceDialog = false
            }
            manualSawProbe && !probing -> {
                if (state.connectionStatus == ConnectionStatus.CONNECTION_FAILED) {
                    manualInlineMessage = state.diagnosticMessage ?: manualAddFailedFallback
                    manualInlineIsError = true
                } else {
                    manualInlineMessage = null
                    showManualDeviceDialog = false
                }
                manualAddAttempt = null
            }
        }
    }

    Scaffold(
        containerColor = NeoBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NeoBottomNavigation(
                selectedTab = state.selectedTab,
                accent = accent,
                onSelectTab = onSelectTab
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(NeoBackgroundDeep, NeoBackground, NeoBackgroundLow)
                    )
                )
                .circuitBoardBackground(accent)
        ) {
            val viewportHeight = maxHeight
            val heroLayout = viewportHeight >= 800.dp && maxWidth >= 380.dp
            val horizontalPadding = if (heroLayout) 10.dp else 8.dp
            val verticalPadding = if (heroLayout) 6.dp else 4.dp
            val sectionSpacing = if (heroLayout) 8.dp else 6.dp
            val tabScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                if (state.selectedTab != RemoteTab.REMOTE) {
                    ActiveDeviceHeader(
                        state = state,
                        ecosystem = ecosystem,
                        accent = accent,
                        onScan = onScan,
                        onRefreshStatus = onRefreshStatus,
                        onOpenSetupGuide = { showSetupGuideDialog = true },
                        onSelect = onSelect,
                        onGoogleSelect = { onSelectGoogleDevice(it.id) },
                        compact = true
                    )
                }

                Crossfade(
                    modifier = Modifier.weight(1f),
                    targetState = state.selectedTab,
                    label = "tabContent"
                ) { tab ->
                    when (tab) {
                        RemoteTab.REMOTE -> {
                            val (showWatchGoogleTv, showWatchRoku) = watchModeVisibility(state)
                            RemoteTabLayout(
                                modifier = Modifier.fillMaxSize(),
                                viewportHeight = viewportHeight,
                                sectionSpacing = sectionSpacing,
                                state = state,
                                ecosystem = ecosystem,
                                accent = accent,
                                accentSoft = accentSoft,
                                showWatchGoogleTv = showWatchGoogleTv,
                                showWatchRoku = showWatchRoku,
                                onScan = onScan,
                                onRefreshStatus = onRefreshStatus,
                                onOpenSetupGuide = { showSetupGuideDialog = true },
                                onSelect = onSelect,
                                onGoogleSelect = { onSelectGoogleDevice(it.id) },
                                onWatchGoogleTv = onWatchGoogleTv,
                                onWatchRoku = onWatchRoku,
                                onDpad = onDpad,
                                onCommand = onCommand,
                                onVolume = onVolume,
                                onLaunchPreset = { id -> onCommand(RemoteCommand.LaunchApp(id)) },
                                onLaunchPinnedApp = onLaunchPinnedApp,
                                onSelectTab = onSelectTab,
                                onTargetChange = onLaunchTargetChange,
                                onLaunch = onLaunch,
                                onSwitchInput = onSwitchTvInput,
                                setupGuideBanner = {
                                    SetupGuideBanner(
                                        state = state,
                                        accent = accent,
                                        onOpenGuide = { showSetupGuideDialog = true },
                                        onHide = onHideConnectionGuide
                                    )
                                }
                            )
                        }
                        RemoteTab.APPS -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(tabScroll),
                            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                        ) {
                            ActiveTvSubtitle(
                                state = state,
                                accent = accent,
                                onOpenRemote = { onSelectTab(RemoteTab.REMOTE) }
                            )
                            AppsPanel(
                                state = state,
                                ecosystem = ecosystem,
                                accent = accent,
                                onRefreshRokuChannels = onRefreshRokuChannels,
                                onPinRokuChannel = onPinRokuChannel,
                                onLaunchPinnedApp = onLaunchPinnedApp,
                                onEditGoogleTvApp = {
                                    editingGoogleTvApp = it
                                    showGoogleTvAppDialog = true
                                },
                                onAddGoogleTvApp = {
                                    editingGoogleTvApp = null
                                    showGoogleTvAppDialog = true
                                },
                                onRemovePinnedApp = { pendingRemoval = PendingRemoval.App(it) }
                            )
                            Spacer(Modifier.height(18.dp))
                        }
                        RemoteTab.SETTINGS -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(tabScroll),
                            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                        ) {
                            ActiveTvSubtitle(
                                state = state,
                                accent = accent,
                                onOpenRemote = { onSelectTab(RemoteTab.REMOTE) }
                            )
                            SettingsPanel(
                                state = state,
                                ecosystem = ecosystem,
                                accent = accent,
                                onScan = onScan,
                                onOpenSetupGuide = { showSetupGuideDialog = true },
                                onOpenManualDevice = { showManualDeviceDialog = true },
                                onRefreshStatus = onRefreshStatus,
                                onRefreshRokuChannels = onRefreshRokuChannels,
                                onOpenGoogleTvSetup = {
                                    state.googleTvDevice?.let { google ->
                                        onSelect(google.id)
                                        showGoogleTvSetupDialog = true
                                        if (!state.pairedDeviceIds.contains(google.id)) {
                                            onStartPairing(google)
                                        }
                                    }
                                },
                                onResetAppPins = onResetAppPins,
                                onOpenApps = { onSelectTab(RemoteTab.APPS) },
                                onSetShowConnectionGuide = onSetShowConnectionGuide,
                                onSetShowUtilitiesDock = onSetShowUtilitiesDock,
                                onSetStartupScreen = onSetStartupScreen,
                                onRemoveSavedDevice = { id ->
                                    state.devices.firstOrNull { device -> device.id == id }
                                        ?.let { device -> pendingRemoval = PendingRemoval.Device(device) }
                                },
                                onDedupeSavedDevices = onDedupeSavedDevices
                            )
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showGoogleTvSetupDialog && googleTv != null) {
        GoogleTvSetupDialog(
            state = state,
            google = googleTv,
            onDismiss = { showGoogleTvSetupDialog = false },
            onSetMapping = { port -> onSetMapping(googleTv, port) },
            onPairingPinChange = onPairingPinChange,
            onStartPairing = { onStartPairing(googleTv) },
            onFinishPairing = { onFinishPairing(googleTv) }
        )
    }

    if (showGoogleTvAppDialog) {
        GoogleTvAppDialog(
            existing = editingGoogleTvApp,
            onDismiss = { showGoogleTvAppDialog = false },
            onSave = { name, target ->
                onSaveGoogleTvApp(name, target)
                showGoogleTvAppDialog = false
            }
        )
    }

    if (showManualDeviceDialog) {
        ManualDeviceDialog(
            isBusy = state.isProbingManualDevice,
            inlineMessage = manualInlineMessage,
            inlineIsError = manualInlineIsError,
            onDismiss = {
                manualAddAttempt = null
                manualInlineMessage = null
                showManualDeviceDialog = false
            },
            onAdd = { type, address, name ->
                manualAddAttempt = ManualAddAttempt(
                    deviceCount = state.devices.size,
                    priorFeedback = state.userFeedback,
                    priorOnline = state.connectionStatus == ConnectionStatus.ONLINE,
                )
                manualSawProbe = false
                manualInlineMessage = null
                manualInlineIsError = false
                onAddManualDevice(type, address, name)
            }
        )
    }

    if (pendingRemoval != null) {
        ConfirmRemovalDialog(
            removal = pendingRemoval!!,
            onDismiss = { pendingRemoval = null },
            onConfirmDevice = onRemoveSavedDevice,
            onConfirmApp = onRemovePinnedApp
        )
    }

    if (showSetupGuideDialog) {
        SetupGuideDialog(
            state = state,
            onDismiss = { showSetupGuideDialog = false },
            onScan = onScan,
            onOpenManualDevice = {
                showSetupGuideDialog = false
                showManualDeviceDialog = true
            },
            onOpenGoogleTvSetup = {
                state.googleTvDevice?.let { google ->
                    onSelect(google.id)
                    showSetupGuideDialog = false
                    showGoogleTvSetupDialog = true
                    if (!state.pairedDeviceIds.contains(google.id)) {
                        onStartPairing(google)
                    }
                }
            }
        )
    }
}

@Composable
private fun SetupGuideBanner(
    state: RGRemoteUiState,
    accent: Color,
    onOpenGuide: () -> Unit,
    onHide: () -> Unit,
) {
    if (!state.showConnectionGuide) return
    if (!state.setupIncomplete) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NeoSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.guide_banner_title),
                            color = PrimaryText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = setupGuideStatusText(state),
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onHide) {
                        Text(stringResource(R.string.guide_banner_hide), color = SecondaryText)
                    }
                    Button(
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        onClick = onOpenGuide
                    ) {
                        Text(stringResource(R.string.guide_banner_guide), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Pending destructive removal awaiting user confirmation. */
private sealed interface PendingRemoval {
    data class Device(val device: RegisteredDevice) : PendingRemoval
    data class App(val app: AppLaunchTarget) : PendingRemoval
}

@Composable
private fun ConfirmRemovalDialog(
    removal: PendingRemoval,
    onDismiss: () -> Unit,
    onConfirmDevice: (String) -> Unit,
    onConfirmApp: (AppLaunchTarget) -> Unit,
) {
    val title: String
    val message: String
    val onConfirm: () -> Unit
    when (removal) {
        is PendingRemoval.Device -> {
            title = stringResource(R.string.removed_device_title)
            message = stringResource(
                R.string.removed_device_confirm,
                removal.device.friendlyName,
                removal.device.ipAddress
            )
            onConfirm = { onConfirmDevice(removal.device.id) }
        }
        is PendingRemoval.App -> {
            title = stringResource(R.string.removed_app_title)
            message = stringResource(R.string.removed_app_confirm, removal.app.displayName)
            onConfirm = { onConfirmApp(removal.app) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.common_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(title) },
        text = { Text(message) },
        containerColor = NeoSurface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText
    )
}

@Composable
private fun SetupGuideDialog(
    state: RGRemoteUiState,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onOpenManualDevice: () -> Unit,
    onOpenGoogleTvSetup: () -> Unit
) {
    val roku = state.rokuDevice
    val rokuReady = roku != null
    val google = state.googleTvDevice
    val mappedPort = google?.hdmiPortMapping
    val googleDiscovered = google != null
    val googlePaired = google?.id?.let { state.pairedDeviceIds.contains(it) } == true
    val hdmiMapped = mappedPort != null
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.common_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenManualDevice) {
                Text(stringResource(R.string.common_add_by_ip))
            }
        },
        title = {
            Text(stringResource(R.string.setup_guide_title))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                SavedTvsSummary(state = state)
                GuideStep(
                    number = "1",
                    title = stringResource(R.string.guide_step_1_title),
                    body = stringResource(R.string.guide_step_1_body),
                    complete = false,
                    accent = RokuPrimary
                )
                GuideStep(
                    number = "2",
                    title = stringResource(R.string.guide_step_2_title),
                    body = stringResource(R.string.guide_step_2_body),
                    complete = false,
                    accent = RokuPrimary
                )
                GuideStep(
                    number = "3",
                    title = stringResource(R.string.guide_step_3_title),
                    body = if (rokuReady) {
                        stringResource(R.string.guide_step_3_body_connected, roku.friendlyName)
                    } else {
                        stringResource(R.string.guide_step_3_body_scan)
                    },
                    complete = rokuReady,
                    accent = RokuPrimary
                )
                GuideStep(
                    number = "4",
                    title = stringResource(R.string.guide_step_4_title),
                    body = when {
                        googlePaired -> stringResource(R.string.guide_step_4_body_paired)
                        googleDiscovered -> stringResource(R.string.guide_step_4_body_discovered)
                        else -> stringResource(R.string.guide_step_4_body_missing)
                    },
                    complete = googlePaired,
                    accent = GooglePrimary
                )
                GuideStep(
                    number = "5",
                    title = stringResource(R.string.guide_step_5_title),
                    body = if (hdmiMapped) {
                        stringResource(R.string.guide_step_5_body_mapped, mappedPort.displayName)
                    } else {
                        stringResource(R.string.guide_step_5_body_pick)
                    },
                    complete = hdmiMapped,
                    accent = GooglePrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = if (state.isScanning) stringResource(R.string.common_scanning) else stringResource(R.string.common_scan),
                        icon = Icons.Default.Refresh,
                        accent = RokuPrimary,
                        enabled = !state.isScanning,
                        onClick = onScan
                    )
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = if (googleDiscovered) stringResource(R.string.guide_pair_hdmi) else stringResource(R.string.common_add_by_ip),
                        icon = if (googleDiscovered) Icons.Default.SmartDisplay else Icons.Default.Add,
                        accent = GooglePrimary,
                        enabled = true,
                        onClick = if (googleDiscovered) onOpenGoogleTvSetup else onOpenManualDevice
                    )
                }
            }
        },
        containerColor = NeoSurface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText
    )
}

@Composable
private fun GuideStep(
    number: String,
    title: String,
    body: String,
    complete: Boolean,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NeoCard.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (complete) 0.48f else 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = if (complete) SuccessGreen.copy(alpha = 0.18f) else accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, if (complete) SuccessGreen else accent.copy(alpha = 0.52f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (complete) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    } else {
                        Text(
                            text = number,
                            color = accent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SavedTvsSummary(state: RGRemoteUiState) {
    if (state.devices.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NeoCard.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.saved_summary_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            state.devices.forEach { device ->
                Text(
                    text = stringResource(
                        R.string.saved_summary_line,
                        device.type.name.replace('_', ' '),
                        device.friendlyName,
                        device.ipAddress
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.devices.duplicateDeviceCount() > 0) {
                Text(
                    text = stringResource(R.string.saved_summary_duplicates_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private fun setupGuideStatusText(state: RGRemoteUiState): String {
    val roku = state.rokuDevice
    val google = state.googleTvDevice
    return when {
        roku == null -> "Start with Roku network access and device discovery."
        google == null -> "Roku is ready. Add or discover the Google TV box when you need it."
        !state.pairedDeviceIds.contains(google.id) -> "Google TV found. Pair it with the PIN shown on the TV."
        google.hdmiPortMapping == null -> "Pairing is done. Map the Google TV HDMI input next."
        else -> "Setup is ready. Test Roku Home and Google TV Home."
    }
}

@Composable
private fun AppsPanel(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    onRefreshRokuChannels: () -> Unit,
    onPinRokuChannel: (RokuApp) -> Unit,
    onLaunchPinnedApp: (AppLaunchTarget) -> Unit,
    onEditGoogleTvApp: (AppLaunchTarget) -> Unit,
    onAddGoogleTvApp: () -> Unit,
    onRemovePinnedApp: (AppLaunchTarget) -> Unit
) {
    val activePins = state.pinnedApps.filter { it.deviceType == ecosystem.type }
    NeoPanel(accent = accent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (ecosystem.type == DeviceType.ROKU_TV) stringResource(R.string.apps_roku_title) else stringResource(R.string.apps_google_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (ecosystem.type == DeviceType.ROKU_TV) {
                            stringResource(R.string.apps_roku_subtitle)
                        } else {
                            stringResource(R.string.apps_google_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                if (ecosystem.type == DeviceType.ROKU_TV) {
                    Button(
                        enabled = !state.isLoadingApps,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        onClick = onRefreshRokuChannels
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isLoadingApps) stringResource(R.string.apps_refreshing) else stringResource(R.string.apps_refresh))
                    }
                } else {
                    Button(
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        onClick = onAddGoogleTvApp
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.common_add))
                    }
                }
            }

            if (activePins.isEmpty()) {
                EmptyAppsHint(
                    text = if (ecosystem.type == DeviceType.ROKU_TV) {
                        stringResource(R.string.apps_no_roku_pins)
                    } else {
                        stringResource(R.string.apps_no_google_pins)
                    },
                    accent = accent
                )
            } else {
                AppButtonGrid(
                    apps = activePins,
                    accent = accent,
                    canEdit = ecosystem.type == DeviceType.GOOGLE_TV,
                    onLaunch = onLaunchPinnedApp,
                    onEdit = onEditGoogleTvApp,
                    onRemove = onRemovePinnedApp
                )
            }

            if (ecosystem.type == DeviceType.ROKU_TV) {
                RokuChannelPicker(
                    apps = state.rokuApps,
                    pins = state.pinnedApps,
                    accent = accent,
                    isLoading = state.isLoadingApps,
                    onRefresh = onRefreshRokuChannels,
                    onPin = onPinRokuChannel
                )
            }
        if (ecosystem.type == DeviceType.GOOGLE_TV && activePins.isEmpty()) {
            Text(
                text = stringResource(R.string.apps_google_empty_hint),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun EmptyAppsHint(text: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NeoCard.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AppButtonGrid(
    apps: List<AppLaunchTarget>,
    accent: Color,
    canEdit: Boolean,
    onLaunch: (AppLaunchTarget) -> Unit,
    onEdit: (AppLaunchTarget) -> Unit,
    onRemove: (AppLaunchTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        apps.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    AppLaunchCard(
                        modifier = Modifier.weight(1f),
                        app = app,
                        accent = accent,
                        canEdit = canEdit,
                        onLaunch = onLaunch,
                        onEdit = onEdit,
                        onRemove = onRemove
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AppLaunchCard(
    modifier: Modifier,
    app: AppLaunchTarget,
    accent: Color,
    canEdit: Boolean,
    onLaunch: (AppLaunchTarget) -> Unit,
    onEdit: (AppLaunchTarget) -> Unit,
    onRemove: (AppLaunchTarget) -> Unit
) {
    Surface(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onLaunch(app) },
        shape = RoundedCornerShape(20.dp),
        color = NeoCard.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = app.displayName,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    if (canEdit) {
                        IconButton(modifier = Modifier.size(48.dp), onClick = { onEdit(app) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.apps_cd_edit_app), tint = SecondaryText, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(modifier = Modifier.size(48.dp), onClick = { onRemove(app) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.apps_cd_remove_app), tint = DangerRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                text = app.launchValue,
                color = SecondaryText.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RokuChannelPicker(
    apps: List<RokuApp>,
    pins: List<AppLaunchTarget>,
    accent: Color,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onPin: (RokuApp) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.channels_available),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            TextButton(enabled = !isLoading, onClick = onRefresh) {
                Text(if (isLoading) stringResource(R.string.channels_refreshing) else stringResource(R.string.channels_refresh))
            }
        }
        if (apps.isEmpty()) {
            EmptyAppsHint(text = stringResource(R.string.channels_empty_hint), accent = accent)
        } else {
            apps.forEach { app ->
                val pinned = pins.any { it.deviceType == DeviceType.ROKU_TV && it.launchValue == app.id }
                RokuChannelRow(app = app, pinned = pinned, accent = accent, onPin = onPin)
            }
        }
    }
}

@Composable
private fun RokuChannelRow(
    app: RokuApp,
    pinned: Boolean,
    accent: Color,
    onPin: (RokuApp) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeoInnerCard,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.name,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.channels_roku_id, app.id),
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                enabled = !pinned,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pinned) NeoCard else accent,
                    contentColor = Color.White,
                    disabledContainerColor = NeoCard,
                    disabledContentColor = SecondaryText
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = { onPin(app) }
            ) {
                Text(if (pinned) stringResource(R.string.channels_pinned) else stringResource(R.string.channels_pin))
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    state: RGRemoteUiState,
    ecosystem: EcosystemStyle,
    accent: Color,
    onScan: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onOpenManualDevice: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRefreshRokuChannels: () -> Unit,
    onOpenGoogleTvSetup: () -> Unit,
    onResetAppPins: () -> Unit,
    onOpenApps: () -> Unit,
    onSetShowConnectionGuide: (Boolean) -> Unit,
    onSetShowUtilitiesDock: (Boolean) -> Unit,
    onSetStartupScreen: (RemoteTab?) -> Unit,
    onRemoveSavedDevice: (String) -> Unit,
    onDedupeSavedDevices: () -> Unit,
) {
    NeoPanel(accent = accent) {
            Text(stringResource(R.string.settings_title), color = PrimaryText, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            SettingsSection(
                title = stringResource(R.string.settings_devices_title),
                body = stringResource(R.string.settings_devices_body),
                accent = accent
            ) {
                SettingsActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.settings_connection_guide),
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    accent = accent,
                    enabled = true,
                    onClick = onOpenSetupGuide
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = if (state.isScanning) stringResource(R.string.common_scanning) else stringResource(R.string.settings_scan_devices),
                        icon = Icons.Default.Refresh,
                        accent = accent,
                        enabled = !state.isScanning,
                        onClick = onScan
                    )
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.settings_refresh_status),
                        icon = Icons.Default.Check,
                        accent = accent,
                        enabled = state.selectedDevice?.type == DeviceType.ROKU_TV ||
                            state.selectedDevice?.type == DeviceType.GOOGLE_TV,
                        onClick = onRefreshStatus
                    )
                }
                SettingsActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.common_add_by_ip),
                    icon = Icons.Default.Add,
                    accent = accent,
                    enabled = true,
                    onClick = onOpenManualDevice
                )
                SettingsActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.settings_merge_duplicates),
                    icon = Icons.Default.Refresh,
                    accent = accent,
                    enabled = true,
                    onClick = onDedupeSavedDevices
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_startup_title),
                body = stringResource(R.string.settings_startup_body),
                accent = accent
            ) {
                val startupChoice = state.startupScreen?.name ?: STARTUP_CHOICE_LAST_SCREEN
                StartupChoiceRow(
                    label = stringResource(R.string.settings_startup_last),
                    selected = startupChoice == STARTUP_CHOICE_LAST_SCREEN,
                    accent = accent,
                    onSelect = {
                        onSetStartupScreen(null)
                    }
                )
                listOf(
                    RemoteTab.REMOTE to stringResource(R.string.settings_startup_remote),
                    RemoteTab.APPS to stringResource(R.string.settings_startup_apps),
                    RemoteTab.SETTINGS to stringResource(R.string.settings_startup_settings)
                ).forEach { (tab, label) ->
                    StartupChoiceRow(
                        label = label,
                        selected = startupChoice == tab.name,
                        accent = accent,
                        onSelect = { onSetStartupScreen(tab) }
                    )
                }
            }
            SettingsSection(
                title = stringResource(R.string.settings_remote_title),
                body = stringResource(R.string.settings_remote_body),
                accent = accent
            ) {
                PreferenceSwitchRow(
                    label = stringResource(R.string.settings_pref_guide_banner),
                    checked = state.showConnectionGuide,
                    accent = accent,
                    onCheckedChange = onSetShowConnectionGuide
                )
                PreferenceSwitchRow(
                    label = stringResource(R.string.settings_pref_utilities),
                    checked = state.showUtilitiesDock,
                    accent = accent,
                    onCheckedChange = onSetShowUtilitiesDock
                )
            }
            if (state.devices.isNotEmpty()) {
                SettingsSection(
                    title = stringResource(R.string.settings_saved_title),
                    body = stringResource(R.string.settings_saved_body),
                    accent = accent
                ) {
                    state.devices.forEach { device ->
                        SavedDeviceRow(
                            device = device,
                            accent = if (device.type == DeviceType.ROKU_TV) RokuPrimary else GooglePrimary,
                            onRemove = { onRemoveSavedDevice(device.id) }
                        )
                    }
                }
            }
            SettingsSection(
                title = stringResource(R.string.settings_active_title),
                body = state.selectedDevice?.let { "${it.friendlyName} / ${it.ipAddress}:${it.port}" } ?: stringResource(R.string.settings_active_none),
                accent = accent
            ) {
                if (ecosystem.type == DeviceType.GOOGLE_TV) {
                    SettingsActionButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = if (state.isGoogleTvPaired) stringResource(R.string.settings_google_pairing_hdmi) else stringResource(R.string.settings_pair_google),
                        icon = Icons.Default.SmartDisplay,
                        accent = GooglePrimary,
                        enabled = state.googleTvDevice != null,
                        onClick = onOpenGoogleTvSetup
                    )
                }
            }
            SettingsSection(
                title = stringResource(R.string.settings_privacy_title),
                body = stringResource(R.string.settings_privacy_body),
                accent = accent
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_note),
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_diagnostics_title),
                body = buildString {
                    append("Status: ${state.connectionStatus.label}")
                    state.selectedDevice?.let { device ->
                        append("\nDevice: ${device.friendlyName}")
                        append("\nEndpoint: ${device.ipAddress}:${device.port}")
                        append("\nFailures: ${device.consecutiveFailures}")
                        if (device.type == DeviceType.ROKU_TV) {
                            state.selectedRokuPowerMode?.let { raw ->
                                val label = RokuPowerMode.displayLabel(raw)
                                append("\nPower mode: ${label ?: raw}")
                            }
                        }
                    }
                    state.diagnosticMessage?.let { detail ->
                        append("\nDetail: $detail")
                    }
                },
                accent = accent
            ) {
                Text(
                    text = stringResource(R.string.settings_diagnostics_note),
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_app_buttons_title),
                body = stringResource(R.string.settings_app_buttons_body),
                accent = accent
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.settings_open_apps),
                        icon = Icons.Default.Apps,
                        accent = accent,
                        enabled = true,
                        onClick = onOpenApps
                    )
                    SettingsActionButton(
                        modifier = Modifier.weight(1f),
                        label = if (ecosystem.type == DeviceType.ROKU_TV) stringResource(R.string.channels_refresh) else stringResource(R.string.settings_reset_buttons),
                        icon = if (ecosystem.type == DeviceType.ROKU_TV) Icons.Default.Refresh else Icons.Default.Delete,
                        accent = if (ecosystem.type == DeviceType.ROKU_TV) accent else DangerRed,
                        enabled = true,
                        onClick = if (ecosystem.type == DeviceType.ROKU_TV) onRefreshRokuChannels else onResetAppPins
                    )
                }
            }
    }
}

@Composable
private fun PreferenceSwitchRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryText,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private const val STARTUP_CHOICE_LAST_SCREEN = "last"

@Composable
private fun StartupChoiceRow(
    label: String,
    selected: Boolean,
    accent: Color,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryText,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = SecondaryText)
        )
    }
}

/** Snapshot taken when Add-by-IP is submitted so probe completion can be classified as success or failure. */
private data class ManualAddAttempt(
    val deviceCount: Int,
    val priorFeedback: String?,
    val priorOnline: Boolean,
)

@Composable
private fun SavedDeviceRow(
    device: RegisteredDevice,
    accent: Color,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NeoCard.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = device.friendlyName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.ipAddress}:${device.port}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.common_remove), color = DangerRed)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    body: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NeoCard.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(body, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun SettingsActionButton(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = NeoSurface,
            disabledContentColor = SecondaryText.copy(alpha = 0.52f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.78f else 0.18f)),
        contentPadding = PaddingValues(horizontal = 10.dp),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}








@Composable
private fun ManualDeviceDialog(
    isBusy: Boolean,
    inlineMessage: String?,
    inlineIsError: Boolean,
    onDismiss: () -> Unit,
    onAdd: (DeviceType, String, String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(DeviceType.ROKU_TV.name) }
    var address by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val type = DeviceType.valueOf(selectedType)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = address.trim().isNotBlank() && !isBusy,
                onClick = { onAdd(type, address, name) }
            ) {
                Text(if (isBusy) stringResource(R.string.manual_checking) else stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = {
            Text(stringResource(R.string.manual_add_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(DeviceType.ROKU_TV to stringResource(R.string.ecosystem_roku), DeviceType.GOOGLE_TV to stringResource(R.string.ecosystem_google_tv)).forEach { (candidate, label) ->
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == candidate) RokuPrimary else NeoCard,
                                contentColor = if (type == candidate) Color.White else SecondaryText
                            ),
                            onClick = { selectedType = candidate.name }
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Text(
                    text = if (type == DeviceType.ROKU_TV) {
                        stringResource(R.string.manual_hint_roku)
                    } else {
                        stringResource(R.string.manual_hint_google)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.manual_label_address)) },
                    placeholder = { Text(if (type == DeviceType.ROKU_TV) stringResource(R.string.manual_placeholder_roku) else stringResource(R.string.manual_placeholder_google)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.manual_label_name)) },
                    placeholder = { Text(if (type == DeviceType.ROKU_TV) stringResource(R.string.manual_placeholder_name_roku) else stringResource(R.string.device_onn_google_tv)) }
                )
                if (inlineMessage != null) {
                    Text(
                        text = inlineMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inlineIsError) DangerRed else SecondaryText
                    )
                }
            }
        },
        containerColor = NeoSurface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText
    )
}

@Composable
private fun GoogleTvAppDialog(
    existing: AppLaunchTarget?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.displayName.orEmpty()) }
    var target by rememberSaveable(existing?.id) { mutableStateOf(existing?.launchValue.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = name.trim().isNotBlank() && target.trim().isNotBlank(),
                onClick = { onSave(name, target) }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = {
            Text(if (existing == null) stringResource(R.string.gtapp_add_title) else stringResource(R.string.gtapp_edit_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.gtapp_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.gtapp_label_name)) },
                    placeholder = { Text(stringResource(R.string.gtapp_placeholder_name)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = target,
                    onValueChange = { target = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.launch_placeholder_package)) },
                    placeholder = { Text(stringResource(R.string.gtapp_placeholder_target)) }
                )
            }
        },
        containerColor = NeoSurface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText
    )
}

@Composable
private fun GoogleTvSetupDialog(
    state: RGRemoteUiState,
    google: RegisteredDevice,
    onDismiss: () -> Unit,
    onSetMapping: (HdmiPort) -> Unit,
    onPairingPinChange: (String) -> Unit,
    onStartPairing: () -> Unit,
    onFinishPairing: () -> Unit
) {
    val isPaired = state.pairedDeviceIds.contains(google.id)
    val sessionStarted = state.pairingSessionDeviceId == google.id
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_done))
            }
        },
        title = {
            Text(stringResource(R.string.gtsetup_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isPaired) {
                        stringResource(R.string.gtsetup_paired_msg)
                    } else if (sessionStarted) {
                        stringResource(R.string.gtsetup_session_msg)
                    } else {
                        stringResource(R.string.gtsetup_start_msg)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = state.pairingPin,
                        onValueChange = onPairingPinChange,
                        singleLine = true,
                        label = { Text(stringResource(R.string.gtsetup_label_code)) }
                    )
                    Button(
                        modifier = Modifier.height(56.dp),
                        enabled = !state.isPairing,
                        onClick = onStartPairing
                    ) {
                        Text(if (isPaired) stringResource(R.string.gtsetup_re_pair) else stringResource(R.string.gtsetup_start))
                    }
                    Button(
                        modifier = Modifier.height(56.dp),
                        enabled = sessionStarted && state.pairingPin.length == 6 && !state.isPairing,
                        onClick = onFinishPairing
                    ) {
                        Text(stringResource(R.string.gtsetup_pair))
                    }
                }
                Text(
                    text = stringResource(R.string.gtsetup_expiry_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Text(
                    text = stringResource(R.string.gtsetup_hdmi_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                listOf(HdmiPort.HDMI1, HdmiPort.HDMI2, HdmiPort.HDMI3, HdmiPort.HDMI4).forEach { port ->
                    val selected = google.hdmiPortMapping == port
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) GooglePrimary else NeoCard,
                            contentColor = if (selected) Color.White else SecondaryText
                        ),
                        onClick = { onSetMapping(port) }
                    ) {
                        Text(if (selected) stringResource(R.string.gtsetup_port_selected, port.displayName) else port.displayName)
                    }
                }
            }
        },
        containerColor = NeoSurface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText
    )
}
