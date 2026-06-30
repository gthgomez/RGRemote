package com.rgremote.app.ui.controller

import com.rgremote.app.data.registry.DeviceRegistry
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

class RokuStatusPoller(
    private val scope: CoroutineScope,
    private val registry: DeviceRegistry,
    private val rokuConnection: RokuConnectionCoordinator,
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit
) {
    private var pollingJob: Job? = null
    private val statusMutex = Mutex()

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                delay(5000)
                val state = getState()
                val selected = state.selectedDevice
                if (selected != null && selected.type == DeviceType.ROKU_TV) {
                    if (!state.isScanning && !state.isLoadingApps && state.connectionStatus != ConnectionStatus.CHECKING) {
                        pollRokuStatus(selected)
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun pollRokuStatus(device: RegisteredDevice) {
        statusMutex.withLock {
            rokuConnection.probeDeviceInfo(device)
            .onSuccess { info ->
                val resolved = rokuConnection.resolveDevice(device)
                registry.markCommandSuccess(resolved)
                updateRokuControlAvailability(resolved.id, info)
                updateRokuPowerMode(resolved.id, info.powerMode)
                rokuConnection.queryActiveApp(resolved)
                    .onSuccess { app ->
                        updateState {
                            it.copy(
                                activeApp = app,
                                connectionStatus = ConnectionStatus.ONLINE,
                                diagnosticMessage = rokuConnectedDiagnostic(info.powerMode, app?.name)
                            )
                        }
                    }
                    .onFailure {
                        updateState {
                            it.copy(
                                connectionStatus = ConnectionStatus.ONLINE,
                                diagnosticMessage = rokuConnectedDiagnostic(info.powerMode, activeAppName = null)
                            )
                        }
                    }
            }
            .onFailure { error ->
                registry.markCommandFailure(device)
                updateState {
                    it.copy(
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
        statusMutex.withLock {
            updateState(withStatus(ConnectionStatus.CHECKING))
            updateState(withDiagnostic("Checking Roku ECP..."))
            rokuConnection.probeDeviceInfo(roku)
                .onSuccess { info ->
                    val resolved = rokuConnection.resolveDevice(roku)
                    registry.markCommandSuccess(resolved)
                    updateRokuControlAvailability(resolved.id, info)
                    updateRokuPowerMode(resolved.id, info.powerMode)
                    updateState(withStatus(ConnectionStatus.ONLINE))
                    rokuConnection.queryActiveApp(resolved)
                        .onSuccess { app ->
                            updateState {
                                it.copy(
                                    activeApp = app,
                                    diagnosticMessage = rokuConnectedDiagnostic(
                                        info.powerMode,
                                        app?.name
                                    ),
                                )
                            }
                        }
                        .onFailure {
                            updateState(withDiagnostic(
                                rokuConnectedDiagnostic(info.powerMode, activeAppName = null)
                            ))
                        }
                }
                .onFailure { error ->
                    registry.markCommandFailure(roku)
                    updateState(withStatus(ConnectionStatus.CONNECTION_FAILED))
                    updateState(withDiagnostic("Roku ECP check failed: ${error.displayMessage()}"))
                }
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
