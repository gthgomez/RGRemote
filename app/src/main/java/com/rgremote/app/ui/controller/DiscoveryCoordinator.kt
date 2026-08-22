package com.rgremote.app.ui.controller

import android.content.Context
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.discovery.RokuSsdpListener
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.roku.diagnosticDetail
import com.rgremote.app.ui.ConnectionStatus
import com.rgremote.app.ui.RGRemoteUiState
import com.rgremote.app.ui.withDiagnostic
import com.rgremote.app.ui.withStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DiscoveryCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val discoveryService: DiscoveryService,
    private val registry: DeviceRegistry,
    private val getState: () -> RGRemoteUiState,
    private val updateState: ((RGRemoteUiState) -> RGRemoteUiState) -> Unit,
    private val onRokuDiscovered: suspend (RegisteredDevice) -> Unit
) {
    private var startupDiscoveryStarted = false
    private var ssdpDedupeJob: Job? = null

    private val ssdpListener = RokuSsdpListener(context) { device ->
        scope.launch {
            registry.upsertDiscoveredDevice(device)
            scheduleDedupe()
        }
    }

    fun startSsdpListener() {
        ssdpListener.start(scope)
    }

    fun stopSsdpListener() {
        ssdpListener.stop()
        ssdpDedupeJob?.cancel()
        ssdpDedupeJob = null
    }

    /**
     * Trigger a debounced dedupe from outside the SSDP listener path
     * (e.g., Google TV NSD discovery via [RGRemoteViewModel]'s saveAsync).
     */
    fun requestDedupe() {
        scheduleDedupe()
    }

    fun stopDiscovery() {
        discoveryService.stopGoogleTvDiscovery()
        updateState { it.copy(googleDiscoveryRunning = false) }
    }

    fun scan() {
        scope.launch {
            updateState(withStatus(ConnectionStatus.CHECKING))
            updateState(withDiagnostic("Searching your network for Roku TVs"))
            updateState { it.copy(isScanning = true) }
            val scanResult = runCatching { discoveryService.scanRoku() }
            val failed = scanResult.isFailure
            scanResult.onFailure { error ->
                updateState(withStatus(ConnectionStatus.CONNECTION_FAILED))
                updateState(withDiagnostic("Roku scan failed: ${error.diagnosticDetail()}"))
            }
            val count = scanResult.getOrDefault(0)
            if (!failed) {
                val discoveredRoku = if (count > 0) {
                    registry.devices.first().firstOrNull { it.type == DeviceType.ROKU_TV }
                } else {
                    null
                }
                updateState {
                    it.copy(
                        isScanning = false,
                        selectedDeviceId = it.selectedDeviceId ?: discoveredRoku?.id,
                        connectionStatus = when {
                            count == 0 -> ConnectionStatus.OFFLINE
                            discoveredRoku != null -> ConnectionStatus.CHECKING
                            else -> ConnectionStatus.OFFLINE
                        },
                        diagnosticMessage = when {
                            count == 0 ->
                                "No Roku TVs answered the scan. Check that your phone and TV " +
                                    "are on the same Wi-Fi, then scan again."
                            discoveredRoku != null -> "Found a Roku TV — checking your saved device"
                            else -> "Found $count Roku device(s), but none matched your saved TV"
                        },
                    )
                }
                runCatching { registry.dedupeStoredDevices() }
                discoveredRoku?.let { onRokuDiscovered(it) }
            } else {
                updateState { it.copy(isScanning = false) }
            }
        }
    }

    fun startGoogleDiscovery() {
        runCatching {
            discoveryService.startGoogleTvDiscovery { error ->
                updateState { it.copy(googleDiscoveryRunning = false) }
                updateState(withStatus(ConnectionStatus.CONNECTION_FAILED))
                updateState(withDiagnostic(error))
            }
        }
            .onSuccess { updateState { it.copy(googleDiscoveryRunning = true) } }
            .onFailure {
                updateState(withStatus(ConnectionStatus.CONNECTION_FAILED))
                updateState(withDiagnostic("Google TV search failed: ${it.message}"))
            }
    }

    fun onNearbyWifiPermissionResult(granted: Boolean) {
        if (startupDiscoveryStarted) return
        startupDiscoveryStarted = true
        updateState {
            it.copy(
                connectionStatus = ConnectionStatus.CHECKING,
                diagnosticMessage = if (granted) {
                    "Nearby Wi-Fi allowed — searching for TVs"
                } else {
                    "Nearby Wi-Fi permission denied — finding TVs may not work"
                },
            )
        }
        startGoogleDiscovery()
        scan()
    }

    /**
     * Debounced dedupe: cancel any in-flight dedupe and restart a 2-second
     * window.  Used by both SSDP and Google TV NSD discovery paths.
     */
    private fun scheduleDedupe() {
        ssdpDedupeJob?.cancel()
        ssdpDedupeJob = scope.launch {
            delay(SSDP_DEDUPE_DEBOUNCE_MILLIS)
            registry.dedupeStoredDevices()
        }
    }

    companion object {
        private const val SSDP_DEDUPE_DEBOUNCE_MILLIS = 2_000L
    }
}
