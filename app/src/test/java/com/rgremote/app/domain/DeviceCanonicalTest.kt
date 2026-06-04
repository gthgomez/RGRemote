package com.rgremote.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCanonicalTest {
    @Test
    fun canonicalDevicesPerType_keepsOnePerType() {
        val rokuA = device(DeviceType.ROKU_TV, "roku:a", "192.168.1.10", 1_000L)
        val rokuB = device(DeviceType.ROKU_TV, "roku:b", "192.168.1.10", 2_000L)
        val google = device(DeviceType.GOOGLE_TV, "googletv:x", "192.168.1.20", 3_000L)

        val canonical = listOf(rokuA, rokuB, google).canonicalDevicesPerType()

        assertEquals(2, canonical.size)
        assertTrue(canonical.any { it.id == "roku:b" })
        assertTrue(canonical.any { it.id == "googletv:x" })
    }

    @Test
    fun duplicateDeviceCount_reportsExtras() {
        val devices = listOf(
            device(DeviceType.ROKU_TV, "roku:a", "192.168.1.10", 1_000L),
            device(DeviceType.ROKU_TV, "roku:b", "192.168.1.10", 2_000L),
        )
        assertEquals(1, devices.duplicateDeviceCount())
    }

    private fun device(type: DeviceType, id: String, ip: String, seen: Long): RegisteredDevice =
        RegisteredDevice(
            id = id,
            type = type,
            ipAddress = ip,
            port = if (type == DeviceType.ROKU_TV) 8060 else 6466,
            uniqueId = id,
            friendlyName = "TV",
            lastSeenMillis = seen,
            hdmiPortMapping = null,
            isOnline = true,
            consecutiveFailures = 0
        )
}
