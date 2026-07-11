package com.rgremote.app.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rgremote.app.data.apps.AppPinStore
import com.rgremote.app.data.preferences.RemoteUiPreferences
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.canonicalDevicesPerType
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.domain.ActiveApp
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.AppTargetSource
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.ManualDeviceEndpoint
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteAdapter
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.google.GoogleTvPairingManager
import com.rgremote.app.roku.RokuEcpClient
import com.rgremote.app.roku.RokuConnectionCoordinator
import com.rgremote.app.roku.displayMessage
import com.rgremote.app.ui.controller.DeviceCommandExecutor
import com.rgremote.app.ui.controller.DiscoveryCoordinator
import com.rgremote.app.ui.controller.PairingOrchestrator
import com.rgremote.app.ui.controller.RokuStatusPoller
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RemoteTab {
    REMOTE,
    APPS,
    SETTINGS
}

enum class ConnectionStatus(val label: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    CHECKING("Checking"),
    PAIRED("Paired"),
    NOT_PAIRED("Not paired"),
    CONNECTION_FAILED("Connection failed"),
    WAKE_UNAVAILABLE("Wake unavailable")
}

data class RokuControlAvailability(
    val supportsPowerOff: Boolean? = null,
    val supportsVolume: Boolean? = null,
    val supportsInputSwitching: Boolean? = null,
) {
    fun supports(command: RemoteCommand): Boolean =
        when (command) {
            RemoteCommand.PowerOff -> supportsPowerOff != false
            is RemoteCommand.Volume -> supportsVolume != false
            is RemoteCommand.SetInput -> supportsInputSwitching != false
            else -> true
        }
}

data class RGRemoteUiState(
    val devices: List<RegisteredDevice> = emptyList(),
    val pairedDeviceIds: Set<String> = emptySet(),
    val pinnedApps: List<AppLaunchTarget> = emptyList(),
    val selectedDeviceId: String? = null,
    val activeApp: ActiveApp? = null,
    val selectedTab: RemoteTab = RemoteTab.REMOTE,
    val rokuApps: List<RokuApp> = emptyList(),
    val isScanning: Boolean = false,
    val isLoadingApps: Boolean = false,
    val isPairing: Boolean = false,
    val pairingSessionDeviceId: String? = null,
    val pairingPin: String = "",
    val launchTarget: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.CHECKING,
    val diagnosticMessage: String? = null,
    val userFeedback: String? = null,
    val showFeedback: Boolean = false,
    val googleDiscoveryRunning: Boolean = false,
    val rokuControlsByDeviceId: Map<String, RokuControlAvailability> = emptyMap(),
    val rokuPowerModeByDeviceId: Map<String, String> = emptyMap(),
    val showConnectionGuide: Boolean = true,
    val showUtilitiesDock: Boolean = true,
) {
    val inferredHdmiPort: HdmiPort?
        get() = activeApp?.inferredHdmiPort

    val inputHintText: String?
        get() = inferredHdmiPort?.let { port -> "Likely input: ${port.displayName} (hint)" }
    val selectedDevice: RegisteredDevice? =
        devices.firstOrNull { it.id == selectedDeviceId } ?: devices.firstOrNull()

    val rokuDevice: RegisteredDevice? =
        devices.firstOrNull { it.type == DeviceType.ROKU_TV }

    val googleTvDevice: RegisteredDevice? =
        devices.firstOrNull { it.type == DeviceType.GOOGLE_TV }

    val isGoogleTvPaired: Boolean =
        googleTvDevice?.id?.let { pairedDeviceIds.contains(it) } == true

    val selectedRokuControls: RokuControlAvailability
        get() {
            val device = if (selectedDevice?.type == DeviceType.ROKU_TV) selectedDevice else rokuDevice
            return device?.id?.let { rokuControlsByDeviceId[it] } ?: RokuControlAvailability()
        }

    val selectedRokuPowerMode: String?
        get() {
            val device = if (selectedDevice?.type == DeviceType.ROKU_TV) selectedDevice else rokuDevice
            return device?.id?.let { rokuPowerModeByDeviceId[it] }
        }
}

class RGRemoteViewModel(
    private val context: Context,
    private val registry: DeviceRegistry,
    private val discoveryService: DiscoveryService,
    private val rokuAdapter: RokuEcpClient,
    private val googleTvAdapter: RemoteAdapter,
    private val pairingManager: GoogleTvPairingManager,
    private val appPinStore: AppPinStore,
    private val uiPreferences: RemoteUiPreferences,
) : ViewModel() {
    private val localState = MutableStateFlow(RGRemoteUiState())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val rokuConnection = RokuConnectionCoordinator(discoveryService, registry, rokuAdapter)

    private val feedback = CommandFeedbackController(
        scope = viewModelScope,
        updateState = { action -> localState.update(action) }
    )

    // Extracted Domain Controllers
    private val commandExecutor = DeviceCommandExecutor(
        scope = viewModelScope,
        registry = registry,
        rokuConnection = rokuConnection,
        googleTvAdapter = googleTvAdapter,
        getState = { uiState.value },
        updateState = { action -> localState.update(action) },
        onCommandSuccess = { device, command -> handleCommandSuccess(device, command) },
        feedback = feedback,
    )

    private val pairingOrchestrator = PairingOrchestrator(
        scope = viewModelScope,
        pairingManager = pairingManager,
        getState = { uiState.value },
        updateState = { action -> localState.update(action) }
    )

    private val discoveryCoordinator = DiscoveryCoordinator(
        context = context,
        scope = viewModelScope,
        discoveryService = discoveryService,
        registry = registry,
        getState = { uiState.value },
        updateState = { action -> localState.update(action) },
        onRokuDiscovered = { rokuStatusPoller.refreshRokuStatus(it) }
    )

    private val rokuStatusPoller = RokuStatusPoller(
        scope = viewModelScope,
        registry = registry,
        rokuConnection = rokuConnection,
        getState = { uiState.value },
        updateState = { action -> localState.update(action) }
    )

    val uiState: StateFlow<RGRemoteUiState> =
        combine(
            combine(
                registry.devices,
                registry.pairedDeviceIds,
                appPinStore.pinnedApps,
            ) { devices, pairedIds, pins ->
                Triple(devices, pairedIds, pins)
            },
            combine(
                uiPreferences.showConnectionGuide,
                uiPreferences.showUtilitiesDock,
                localState,
            ) { showGuide, showUtilities, state ->
                Triple(showGuide, showUtilities, state)
            },
        ) { deviceSnapshot, uiSnapshot ->
            val (devices, pairedIds, pins) = deviceSnapshot
            val (showGuide, showUtilities, state) = uiSnapshot
            val canonical = devices.canonicalDevicesPerType()
            val persistedId = uiPreferences.selectedDeviceId
            val selected = state.selectedDeviceId
                ?: persistedId?.takeIf { id -> canonical.any { it.id == id } }
                ?: canonical.firstOrNull()?.id
            state.copy(
                devices = canonical,
                pairedDeviceIds = pairedIds,
                pinnedApps = pins,
                selectedDeviceId = selected,
                showConnectionGuide = showGuide,
                showUtilitiesDock = showUtilities,
            )
        }
        .onStart {
            startForegroundPolling()
        }
        .onCompletion {
            stopForegroundPolling()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RGRemoteUiState())

    init {
        viewModelScope.launch { registry.dedupeStoredDevices() }
        discoveryService.saveAsync = { device ->
            viewModelScope.launch {
                registry.upsertDiscoveredDevice(device)
                discoveryCoordinator.requestDedupe()
            }
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                googleTvAdapter.invalidateSession()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                googleTvAdapter.invalidateSession()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun handleCommandSuccess(device: RegisteredDevice, command: RemoteCommand) {
        if (device.type == DeviceType.ROKU_TV) {
            viewModelScope.launch { rokuStatusPoller.refreshRokuStatus(device) }
        } else {
            refreshStatus()
        }
    }

    fun setShowConnectionGuide(show: Boolean) {
        uiPreferences.setShowConnectionGuide(show)
        if (!show) {
            feedback.show("Connection guide hidden. Re-enable in Settings.")
        }
    }

    fun setShowUtilitiesDock(show: Boolean) {
        uiPreferences.setShowUtilitiesDock(show)
        feedback.show(if (show) "Show utilities on Remote" else "Hide utilities on Remote")
    }

    fun removeSavedDevice(deviceId: String) {
        viewModelScope.launch {
            registry.removeDevice(deviceId)
            localState.update { state ->
                if (state.selectedDeviceId == deviceId) {
                    uiPreferences.setSelectedDeviceId(null)
                    state.copy(selectedDeviceId = null, connectionStatus = ConnectionStatus.CHECKING, diagnosticMessage = null)
                } else {
                    state.copy(selectedDeviceId = state.selectedDeviceId)
                }
            }
            feedback.show("Removed saved device")
        }
    }

    fun dedupeSavedDevices() {
        viewModelScope.launch {
            registry.dedupeStoredDevices()
            feedback.show("Merged duplicate saved TVs")
        }
    }

    fun stopDiscovery() {
        discoveryCoordinator.stopDiscovery()
    }

    private fun startForegroundPolling() {
        discoveryCoordinator.startSsdpListener()
        rokuStatusPoller.startPolling()
    }

    private fun stopForegroundPolling() {
        discoveryCoordinator.stopSsdpListener()
        rokuStatusPoller.stopPolling()
    }

    override fun onCleared() {
        super.onCleared()
        rokuAdapter.shutdown()
        googleTvAdapter.shutdown()
        stopDiscovery()
        stopForegroundPolling()
        discoveryService.destroy()
        networkCallback?.let { callback ->
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun selectDevice(deviceId: String) {
        localState.update { it.copy(
            selectedDeviceId = deviceId,
            rokuApps = emptyList(),
            activeApp = null,
            diagnosticMessage = null,
            userFeedback = null,
            showFeedback = false,
        ) }
        feedback.cancel()
        uiPreferences.setSelectedDeviceId(deviceId)
        refreshStatusForDevice(deviceId)
        refreshCurrentTabIfNeeded(deviceId)
    }

    fun clearUserFeedback() {
        feedback.show(null)
    }

    fun selectGoogleDevice(deviceId: String) {
        val exists = uiState.value.devices.any { it.id == deviceId && it.type == DeviceType.GOOGLE_TV }
        if (!exists) {
            feedback.show("Google TV device not found")
            return
        }
        localState.update { it.copy(
            selectedDeviceId = deviceId,
            rokuApps = emptyList(),
            activeApp = null,
            diagnosticMessage = null,
            userFeedback = null,
            showFeedback = false,
            connectionStatus = ConnectionStatus.CHECKING,
        ) }
        feedback.cancel()
        uiPreferences.setSelectedDeviceId(deviceId)
        refreshStatus()
    }

    fun selectTab(tab: RemoteTab) {
        localState.update { it.copy(selectedTab = tab) }
        if ((tab == RemoteTab.APPS) && (uiState.value.selectedDevice?.type == DeviceType.ROKU_TV) && uiState.value.rokuApps.isEmpty()) {
            refreshRokuChannels()
        }
    }

    fun scan() {
        discoveryCoordinator.scan()
    }

    fun startGoogleDiscovery() {
        discoveryCoordinator.startGoogleDiscovery()
    }

    fun onNearbyWifiPermissionResult(granted: Boolean) {
        discoveryCoordinator.onNearbyWifiPermissionResult(granted)
    }

    fun updatePairingPin(pin: String) {
        pairingOrchestrator.updatePairingPin(pin)
    }

    fun updateLaunchTarget(target: String) {
        localState.update { it.copy(launchTarget = target) }
    }

    fun refreshRokuChannels() {
        val roku = uiState.value.rokuDevice ?: return feedback.show("Add a Roku TV first")
        viewModelScope.launch {
            localState.update { it.copy(isLoadingApps = true, connectionStatus = ConnectionStatus.CHECKING, diagnosticMessage = "Refreshing Roku channels") }
            rokuConnection.queryApps(roku)
                .onSuccess { apps ->
                    registry.markCommandSuccess(roku)
                    localState.update {
                        it.copy(
                            rokuApps = apps.sortedBy { app -> app.name.lowercase() },
                            isLoadingApps = false,
                            connectionStatus = ConnectionStatus.ONLINE,
                            diagnosticMessage = "Found ${apps.size} Roku channel(s)",
                        )
                    }
                }
                .onFailure { error ->
                    registry.markCommandFailure(roku)
                    localState.update {
                        it.copy(
                            isLoadingApps = false,
                            connectionStatus = ConnectionStatus.CONNECTION_FAILED,
                            diagnosticMessage = "Roku channel refresh failed: ${error.displayMessage()}",
                        )
                    }
                }
        }
    }

    fun pinRokuChannel(app: RokuApp) {
        appPinStore.upsert(
            AppLaunchTarget(
                id = AppPinStore.targetId(DeviceType.ROKU_TV, app.id),
                deviceType = DeviceType.ROKU_TV,
                displayName = app.name,
                launchValue = app.id,
                source = AppTargetSource.DISCOVERED,
            ),
        )
        feedback.show("Pinned ${app.name}")
    }

    fun saveGoogleTvApp(displayName: String, launchValue: String) {
        val name = displayName.trim()
        val value = launchValue.trim()
        if (name.isBlank() || value.isBlank()) {
            feedback.show("Enter an app name and package or deep link")
            return
        }
        appPinStore.upsert(
            AppLaunchTarget(
                id = AppPinStore.targetId(DeviceType.GOOGLE_TV, value),
                deviceType = DeviceType.GOOGLE_TV,
                displayName = name,
                launchValue = value,
                source = AppTargetSource.CUSTOM,
            ),
        )
        feedback.show("Saved $name")
    }

    fun removePinnedApp(target: AppLaunchTarget) {
        appPinStore.remove(target.id)
        feedback.show("Removed ${target.displayName}")
    }

    fun resetAppPins() {
        appPinStore.resetDefaults()
        feedback.show("Reset app buttons")
    }

    fun launchPinnedApp(target: AppLaunchTarget) {
        val device = uiState.value.selectedDevice
        if (device?.type != target.deviceType) {
            feedback.show("Select ${target.deviceType.displayName()} first")
            return
        }
        send(RemoteCommand.LaunchApp(target.launchValue))
    }

    fun startPairing(device: RegisteredDevice) {
        pairingOrchestrator.startPairing(device)
    }

    fun finishPairing(device: RegisteredDevice) {
        pairingOrchestrator.finishPairing(device)
    }

    fun setHdmiMapping(device: RegisteredDevice, port: HdmiPort?) {
        viewModelScope.launch {
            registry.setHdmiMapping(device.id, port)
            feedback.show("Saved ${device.friendlyName} mapping")
        }
    }

    fun switchTvInput(port: HdmiPort) {
        commandExecutor.switchTvInput(port)
    }

    fun send(command: RemoteCommand) {
        commandExecutor.send(command)
    }

    fun sendDpad(direction: DpadDirection) {
        send(RemoteCommand.Dpad(direction))
    }

    fun sendVolume(command: VolumeCommand) {
        send(RemoteCommand.Volume(command))
    }

    fun launchTypedTarget() {
        val target = uiState.value.launchTarget.trim()
        if (target.isBlank()) return feedback.show("Enter an app id, package, or deep link")
        send(RemoteCommand.LaunchApp(target))
    }

    fun refreshStatus() {
        refreshStatusForDevice(uiState.value.selectedDeviceId)
    }

    private fun refreshStatusForDevice(deviceId: String?) {
        val selected = uiState.value.devices.firstOrNull { it.id == deviceId } ?: return
        if (selected.type == DeviceType.ROKU_TV) {
            viewModelScope.launch { rokuStatusPoller.refreshRokuStatus(selected) }
        } else {
            localState.update { it.copy(connectionStatus = ConnectionStatus.CHECKING) }
        }
    }

    /**
     * If the user is viewing a tab whose content depends on the selected device
     * (e.g., APPS tab showing Roku channels), kick off a refresh so stale data
     * from the previous device isn't displayed.
     */
    private fun refreshCurrentTabIfNeeded(deviceId: String) {
        val state = uiState.value
        val device = state.devices.firstOrNull { it.id == deviceId } ?: return
        when (state.selectedTab) {
            RemoteTab.APPS -> {
                if (device.type == DeviceType.ROKU_TV && state.rokuApps.isEmpty()) {
                    refreshRokuChannels()
                }
            }
            else -> { /* REMOTE and SETTINGS tabs don't need per-device refresh on switch */ }
        }
    }

    fun watchGoogleTv() {
        val state = uiState.value
        val roku = state.rokuDevice ?: return feedback.show("Add a Roku TV first")
        val google = state.googleTvDevice ?: return feedback.show("Add a Google TV first")
        val port = google.hdmiPortMapping ?: return feedback.show("Map the Google TV HDMI port first")
        commandExecutor.enqueueCommand {
            if (commandExecutor.sendTo(roku, RemoteCommand.SetInput(port), showFeedback = false).isFailure) {
                feedback.show("Failed to switch TV input")
                return@enqueueCommand
            }
            delay(800)
            rokuStatusPoller.refreshRokuStatus(roku)
            if (commandExecutor.sendTo(google, RemoteCommand.Home, showFeedback = false).isFailure) {
                feedback.show("Failed to open Google TV")
                return@enqueueCommand
            }
            localState.update { it.copy(selectedDeviceId = google.id) }
            feedback.show("Switched to Google TV")
        }
    }

    fun watchRoku() {
        val roku = uiState.value.rokuDevice ?: return feedback.show("Add a Roku TV first")
        commandExecutor.enqueueCommand {
            if (commandExecutor.sendTo(roku, RemoteCommand.Home, showFeedback = false).isFailure) {
                feedback.show("Failed to switch to Roku")
                return@enqueueCommand
            }
            localState.update { it.copy(selectedDeviceId = roku.id) }
            feedback.show("Switched to Roku")
        }
    }

    fun addManualDevice(type: DeviceType, rawAddress: String, displayName: String) {
        val defaultPort = if (type == DeviceType.ROKU_TV) ROKU_ECP_PORT else GOOGLE_TV_REMOTE_PORT
        val endpoint = runCatching { ManualDeviceEndpoint.parse(rawAddress, defaultPort) }
            .getOrElse { error ->
                localState.update(withStatus(ConnectionStatus.CONNECTION_FAILED))
                feedback.show(error.message ?: "Invalid address")
                return
            }
        viewModelScope.launch {
            when (type) {
                DeviceType.ROKU_TV -> addManualRoku(endpoint, displayName)
                DeviceType.GOOGLE_TV -> addManualGoogleTv(endpoint, displayName)
            }
        }
    }

    private suspend fun addManualRoku(endpoint: ManualDeviceEndpoint, displayName: String) {
        val probeDevice = manualDevice(DeviceType.ROKU_TV, endpoint, displayName, uniqueId = "manual:${endpoint.host}:${endpoint.port}")
        rokuConnection.probeDeviceInfo(probeDevice)
            .onSuccess { info ->
                val uniqueId = info.serialNumber ?: probeDevice.uniqueId
                val device = probeDevice.copy(
                    id = "roku:${uniqueId.lowercase()}",
                    uniqueId = uniqueId,
                    friendlyName = displayName.trim().ifBlank {
                        info.friendlyName ?: "Roku TV ${endpoint.host}"
                    },
                    isOnline = true,
                    consecutiveFailures = 0,
                    wifiMac = info.wifiMac,
                    ethernetMac = info.ethernetMac
                )
                registry.upsertDiscoveredDevice(device)
                discoveryCoordinator.requestDedupe()
                localState.update {
                    it.copy(
                        selectedDeviceId = device.id,
                        connectionStatus = ConnectionStatus.ONLINE,
                        diagnosticMessage = "Roku ECP connected",
                    )
                }
                viewModelScope.launch { rokuStatusPoller.refreshRokuStatus(device) }
            }
            .onFailure { error ->
                localState.update(withStatus(ConnectionStatus.CONNECTION_FAILED))
                localState.update(withDiagnostic("Manual Roku check failed: ${error.displayMessage()}"))
            }
    }

    private suspend fun addManualGoogleTv(endpoint: ManualDeviceEndpoint, displayName: String) {
        val device = manualDevice(
            type = DeviceType.GOOGLE_TV,
            endpoint = endpoint,
            displayName = displayName,
            uniqueId = "manual:${endpoint.host}:${endpoint.port}"
        ).copy(
            id = "googletv:manual:${endpoint.host}:${endpoint.port}".lowercase(),
            friendlyName = displayName.trim().ifBlank { "Google TV ${endpoint.host}" }
        )
        registry.upsertDiscoveredDevice(device)
        discoveryCoordinator.requestDedupe()
        localState.update {
            it.copy(
                selectedDeviceId = device.id,
                connectionStatus = ConnectionStatus.NOT_PAIRED,
                diagnosticMessage = "Added Google TV manually; pairing will verify the remote service",
            )
        }
    }

    private fun manualDevice(
        type: DeviceType,
        endpoint: ManualDeviceEndpoint,
        displayName: String,
        uniqueId: String
    ): RegisteredDevice =
        RegisteredDevice(
            id = "${type.name.lowercase()}:$uniqueId",
            type = type,
            ipAddress = endpoint.host,
            port = endpoint.port,
            uniqueId = uniqueId,
            friendlyName = displayName.trim().ifBlank {
                if (type == DeviceType.ROKU_TV) "Roku TV ${endpoint.host}" else "Google TV ${endpoint.host}"
            },
            lastSeenMillis = System.currentTimeMillis(),
            hdmiPortMapping = null,
            isOnline = true,
            consecutiveFailures = 0
        )


    private fun DeviceType.displayName(): String =
        when (this) {
            DeviceType.ROKU_TV -> "Roku"
            DeviceType.GOOGLE_TV -> "Google TV"
        }

    companion object {
        private const val ROKU_ECP_PORT = 8060
        private const val GOOGLE_TV_REMOTE_PORT = 6466
    }
}
