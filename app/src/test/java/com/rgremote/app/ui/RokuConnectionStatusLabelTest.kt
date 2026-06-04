package com.rgremote.app.ui

import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class RokuConnectionStatusLabelTest {
    @Test
    fun onlineRoku_includesPowerModeLabel() {
        val state = RGRemoteUiState(
            devices = listOf(rokuDevice),
            selectedDeviceId = rokuDevice.id,
            connectionStatus = ConnectionStatus.ONLINE,
            rokuPowerModeByDeviceId = mapOf(rokuDevice.id to "DisplayOff"),
        )

        assertEquals("Online · Standby", rokuConnectionStatusLabel(state))
    }

    @Test
    fun failedRoku_keepsBaseStatusWithoutPowerSuffix() {
        val state = RGRemoteUiState(
            devices = listOf(rokuDevice),
            selectedDeviceId = rokuDevice.id,
            connectionStatus = ConnectionStatus.CONNECTION_FAILED,
            rokuPowerModeByDeviceId = mapOf(rokuDevice.id to "PowerOff"),
        )

        assertEquals("Connection failed", rokuConnectionStatusLabel(state))
    }

    @Test
    fun googleTv_usesConnectionStatusOnly() {
        val google = rokuDevice.copy(
            id = "googletv:test",
            type = DeviceType.GOOGLE_TV,
        )
        val state = RGRemoteUiState(
            devices = listOf(google),
            selectedDeviceId = google.id,
            connectionStatus = ConnectionStatus.PAIRED,
        )

        assertEquals("Paired", rokuConnectionStatusLabel(state))
    }

    private val rokuDevice = RegisteredDevice(
        id = "roku:test",
        type = DeviceType.ROKU_TV,
        ipAddress = "192.168.1.50",
        port = 8060,
        uniqueId = "test",
        friendlyName = "Roku TV",
        lastSeenMillis = 0L,
        hdmiPortMapping = null,
        isOnline = true,
        consecutiveFailures = 0
    )
}
