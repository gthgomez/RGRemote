package com.rgremote.app.ui.controller

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.canonicalRokuIdentity
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RokuDeviceInfo
import com.rgremote.app.roku.RokuConnectionCoordinator
import com.rgremote.app.roku.RokuNetworkAccessException
import com.rgremote.app.roku.RokuPowerMode
import com.rgremote.app.roku.displayMessage
import com.rgremote.app.ui.ConnectionStatus
import com.rgremote.app.ui.RGRemoteUiState
import com.rgremote.app.ui.RokuControlAvailability
import com.rgremote.app.ui.withDiagnostic
import com.rgremote.app.ui.withStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Authoritative writer for the global [com.rgremote.app.ui.ConnectionStatus].
 *
 * Every write is scoped to the device the operation belongs to: results are
 * discarded when the user has switched away from that device since the
 * operation started, preventing cross-device status corruption.
 */
class DeviceStatusWriter(
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit
) {
    fun writeForDevice(deviceId: String, transform: (RGRemoteUiState) -> RGRemoteUiState) {
        updateState { state ->
            val stillSelected = state.selectedDeviceId == deviceId ||
                (
                    state.selectedDeviceId == null &&
                        getState().selectedDeviceId == deviceId
                    )
            if (stillSelected) transform(state) else state
        }
    }
}

class RokuStatusPoller(
    private val scope: CoroutineScope,
    private val registry: DeviceRegistry,
    private val rokuConnection: RokuConnectionCoordinator,
    private val statusWriter: DeviceStatusWriter,
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit
) {
    private var pollingJob: Job? = null
    private val statusMutex = Mutex()
    private val inFlightRefreshDeviceIds: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                delay(5000)
                val state = getState()
                val selected = state.selectedDevice
                if (selected != null && selected.type == DeviceType.ROKU_TV) {
                    if (!state.isScanning && !state.isLoadingApps && state.connectionStatus != ConnectionStatus.CHECKING) {
                        try {
                            pollRokuStatus(selected)
                        } catch (e: Exception) {
                            // Prevent a single poll failure from killing the loop.
                        }
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Core probe pipeline shared by [pollRokuStatus] and [refreshRokuStatus].
     *
     * Executes probeDeviceInfo → resolveDevice → markCommandSuccess →
     * update control/power availability → queryActiveApp → update state
     * with active app and connected diagnostic.
     *
     * On failure, calls [registry.markCommandFailure] but does NOT update
     * connection status or diagnostic — callers set those per their context.
     */
    private suspend fun executeStatusProbe(device: RegisteredDevice): Result<RokuDeviceInfo> {
        return rokuConnection.probeDeviceInfo(device)
            .onSuccess { info ->
                val resolved = resolveFreshRow(device, info)
                registry.markCommandSuccess(resolved)
                updateRokuControlAvailability(resolved.id, info)
                updateRokuPowerMode(resolved.id, info.powerMode)
                rokuConnection.queryActiveApp(resolved)
                    .onSuccess { app ->
                        statusWriter.writeForDevice(resolved.id) { state ->
                            state.copy(
                                activeApp = app,
                                connectionStatus = ConnectionStatus.ONLINE,
                                diagnosticMessage = rokuConnectedDiagnostic(info.powerMode, app?.name)
                            )
                        }
                    }
                    .onFailure {
                        statusWriter.writeForDevice(resolved.id) { state ->
                            state.copy(
                                connectionStatus = ConnectionStatus.ONLINE,
                                diagnosticMessage = rokuConnectedDiagnostic(info.powerMode, activeAppName = null)
                            )
                        }
                    }
            }
            .onFailure { error ->
                registry.markCommandFailure(device)
            }
    }

    /**
     * Resolves the registry row for [device] after a successful identity probe: the probe
     * may have promoted a manual-keyed device to its serial identity, invalidating the
     * original id, so fall back to the serial-keyed row when the original row is gone.
     */
    private suspend fun resolveFreshRow(device: RegisteredDevice, info: RokuDeviceInfo): RegisteredDevice {
        val byOriginalId = rokuConnection.resolveDevice(device)
        if (registry.getDevice(byOriginalId.id) != null) return byOriginalId
        val serial = info.serialNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return byOriginalId
        val identity = canonicalRokuIdentity(
            serialNumber = serial,
            ipAddress = byOriginalId.ipAddress,
            port = byOriginalId.port,
        )
        return registry.getDevice(identity.id) ?: byOriginalId
    }

    suspend fun pollRokuStatus(device: RegisteredDevice) {
        statusMutex.withLock {
            executeStatusProbe(device).onFailure { error ->
                statusWriter.writeForDevice(device.id) { state ->
                    state.copy(
                        connectionStatus = ConnectionStatus.OFFLINE,
                        diagnosticMessage = if (error is RokuNetworkAccessException) {
                            error.message
                        } else {
                            "Roku ECP connection offline"
                        }
                    )
                }
            }
        }
    }

    suspend fun refreshRokuStatus(roku: RegisteredDevice) {
        if (!inFlightRefreshDeviceIds.add(roku.id)) return
        try {
            statusMutex.withLock {
                statusWriter.writeForDevice(roku.id, withStatus(ConnectionStatus.CHECKING))
                statusWriter.writeForDevice(roku.id, withDiagnostic("Checking Roku ECP..."))
                executeStatusProbe(roku)
                    .onSuccess {
                        statusWriter.writeForDevice(roku.id, withStatus(ConnectionStatus.ONLINE))
                    }
                    .onFailure { error ->
                        statusWriter.writeForDevice(roku.id, withStatus(ConnectionStatus.CONNECTION_FAILED))
                        statusWriter.writeForDevice(
                            roku.id,
                            withDiagnostic("Roku ECP check failed: ${error.displayMessage()}")
                        )
                    }
            }
        } finally {
            inFlightRefreshDeviceIds.remove(roku.id)
        }
    }

    private fun updateRokuPowerMode(deviceId: String, raw: String?) {
        if (raw.isNullOrBlank()) return
        updateState { state ->
            state.copy(rokuPowerModeByDeviceId = state.rokuPowerModeByDeviceId + (deviceId to raw))
        }
    }

    private fun updateRokuControlAvailability(deviceId: String, info: RokuDeviceInfo) {
        val availability = RokuControlAvailability(
            supportsPowerOff = info.supportsTvPowerControl ?: info.isTv,
            supportsVolume = info.supportsAudioVolumeControl ?: info.isTv,
            supportsInputSwitching = info.isTv
        )
        updateState { state ->
            state.copy(rokuControlsByDeviceId = state.rokuControlsByDeviceId + (deviceId to availability))
        }
    }

    private fun rokuConnectedDiagnostic(powerMode: String?, activeAppName: String?): String {
        val powerLabel = RokuPowerMode.displayLabel(powerMode)
        return buildString {
            if (powerLabel != null) append("TV $powerLabel")
            if (activeAppName != null) {
                if (isNotEmpty()) append(" · ")
                append("Active: $activeAppName")
            }
            if (isEmpty()) append("Roku ECP connected")
        }
    }

}
