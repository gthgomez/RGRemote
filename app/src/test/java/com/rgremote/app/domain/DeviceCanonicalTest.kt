package com.rgremote.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCanonicalTest {
    @Test
    fun canonicalDevicesPerType_keepsOnePerType() {
        val rokuA = device(DeviceType.ROKU_TV, "roku:a", "192.168.1.10", 1_000L, uniqueId = "serial:roku1")
        val rokuB = device(DeviceType.ROKU_TV, "roku:b", "192.168.1.10", 2_000L, uniqueId = "serial:roku1")
        val google = device(DeviceType.GOOGLE_TV, "googletv:x", "192.168.1.20", 3_000L, uniqueId = "serial:google1")

        val canonical = listOf(rokuA, rokuB, google).canonicalDevicesPerType()

        assertEquals(2, canonical.size)
        assertTrue(canonical.any { it.id == "roku:b" })
        assertTrue(canonical.any { it.id == "googletv:x" })
    }

    @Test
    fun duplicateDeviceCount_reportsExtras() {
        val devices = listOf(
            device(DeviceType.ROKU_TV, "roku:a", "192.168.1.10", 1_000L, uniqueId = "serial:roku1"),
            device(DeviceType.ROKU_TV, "roku:b", "192.168.1.10", 2_000L, uniqueId = "serial:roku1"),
        )
        assertEquals(1, devices.duplicateDeviceCount())
    }

    @Test
    fun canonicalDevicesPerType_preservesDifferentTypesOnSameIp() {
        val rokuManual = device(DeviceType.ROKU_TV, "roku:manual", "192.168.1.50", 1_000L, uniqueId = "manual:192.168.1.50:8060")
        val googleManual = device(DeviceType.GOOGLE_TV, "google:manual", "192.168.1.50", 2_000L, uniqueId = "manual:192.168.1.50:6466")

        val canonical = listOf(rokuManual, googleManual).canonicalDevicesPerType()

        assertEquals(2, canonical.size)
        assertTrue(canonical.any { it.type == DeviceType.ROKU_TV })
        assertTrue(canonical.any { it.type == DeviceType.GOOGLE_TV })
    }

    @Test
    fun deviceComparator_prefersRealSerialMappedOnlineAndHealthy() {
        val manual = device(DeviceType.ROKU_TV, "roku:manual", "192.168.1.10", 5_000L, uniqueId = "manual:192.168.1.10:8060")
        val serial = device(DeviceType.ROKU_TV, "roku:serial", "192.168.1.10", 1_000L, uniqueId = "serial:12345")
        assertTrue(DEVICE_COMPARATOR.compare(manual, serial) < 0)

        val unmapped = device(DeviceType.ROKU_TV, "roku:unmapped", "192.168.1.10", 5_000L, uniqueId = "serial:1")
        val mapped = unmapped.copy(hdmiPortMapping = HdmiPort.HDMI1, lastSeenMillis = 1_000L)
        assertTrue(DEVICE_COMPARATOR.compare(unmapped, mapped) < 0)

        val offline = mapped.copy(isOnline = false, lastSeenMillis = 5_000L)
        assertTrue(DEVICE_COMPARATOR.compare(offline, mapped) < 0)

        val failing = mapped.copy(consecutiveFailures = 2, lastSeenMillis = 5_000L)
        assertTrue(DEVICE_COMPARATOR.compare(failing, mapped) < 0)
    }

    private fun device(
        type: DeviceType,
        id: String,
        ip: String,
        seen: Long,
        uniqueId: String = id
    ): RegisteredDevice =
        RegisteredDevice(
            id = id,
            type = type,
            ipAddress = ip,
            port = if (type == DeviceType.ROKU_TV) 8060 else 6466,
            uniqueId = uniqueId,
            friendlyName = "TV",
            lastSeenMillis = seen,
            hdmiPortMapping = null,
            isOnline = true,
            consecutiveFailures = 0
        )
}
