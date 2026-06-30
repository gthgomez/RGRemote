package com.rgremote.app.ui.controller

import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.google.GoogleTvPairingManager
import com.rgremote.app.ui.ConnectionStatus
import com.rgremote.app.ui.RGRemoteUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PairingOrchestrator(
    private val scope: CoroutineScope,
    private val pairingManager: GoogleTvPairingManager,
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit
) {
    fun updatePairingPin(pin: String) {
        updateState { it.copy(pairingPin = pin.take(6).uppercase()) }
    }

    fun startPairing(device: RegisteredDevice) {
        if (getState().isPairing) return
        val isRepairing = getState().pairedDeviceIds.contains(device.id)
        updateState {
            it.copy(
                isPairing = true,
                pairingSessionDeviceId = null,
                pairingPin = "",
                connectionStatus = ConnectionStatus.CHECKING,
                diagnosticMessage = if (isRepairing) "Re-pairing Google TV" else "Starting Google TV pairing",
            )
        }
        scope.launch {
            try {
                if (isRepairing) {
                    pairingManager.forgetPairing(device.id)
                } else {
                    pairingManager.cancelPairing(device.id)
                }
                pairingManager.startPairing(device)
                    .onSuccess {
                        updateState { state ->
                            state.copy(
                                pairingSessionDeviceId = device.id,
                                connectionStatus = ConnectionStatus.NOT_PAIRED,
                                diagnosticMessage = "Enter the 6-character PIN shown on Google TV"
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState { state ->
                            state.copy(
                                pairingSessionDeviceId = null,
                                connectionStatus = ConnectionStatus.CONNECTION_FAILED,
                                diagnosticMessage = "Pairing start failed: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                updateState { state ->
                    state.copy(
                        pairingSessionDeviceId = null,
                        connectionStatus = ConnectionStatus.CONNECTION_FAILED,
                        diagnosticMessage = "Pairing start failed: ${e.message}"
                    )
                }
            } finally {
                updateState { it.copy(isPairing = false) }
            }
        }
    }

    fun finishPairing(device: RegisteredDevice) {
        scope.launch {
            try {
                val pin = getState().pairingPin
                updateState {
                    it.copy(
                        isPairing = true,
                        connectionStatus = ConnectionStatus.CHECKING,
                        diagnosticMessage = "Completing Google TV pairing",
                    )
                }
                pairingManager.finishPairing(device, pin)
                    .onSuccess {
                        updateState { state ->
                            state.copy(
                                pairingPin = "",
                                pairingSessionDeviceId = null,
                                connectionStatus = ConnectionStatus.PAIRED,
                                diagnosticMessage = "Google TV paired"
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState { state ->
                            state.copy(
                                pairingSessionDeviceId = null,
                                connectionStatus = ConnectionStatus.CONNECTION_FAILED,
                                diagnosticMessage = "Pairing failed: ${error.message}. Tap Start for a new code."
                            )
                        }
                    }
            } finally {
                updateState { it.copy(isPairing = false) }
            }
        }
    }
}
