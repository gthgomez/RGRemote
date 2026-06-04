package com.rgremote.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ManualDeviceEndpointTest {
    @Test
    fun hostWithoutPortUsesDefaultPort() {
        val endpoint = ManualDeviceEndpoint.parse("192.168.1.25", defaultPort = 8060)

        assertEquals("192.168.1.25", endpoint.host)
        assertEquals(8060, endpoint.port)
    }

    @Test
    fun hostWithPortOverridesDefaultPort() {
        val endpoint = ManualDeviceEndpoint.parse("192.168.1.25:6466", defaultPort = 8060)

        assertEquals("192.168.1.25", endpoint.host)
        assertEquals(6466, endpoint.port)
    }

    @Test
    fun urlFormIsAccepted() {
        val endpoint = ManualDeviceEndpoint.parse("http://roku.local:8060", defaultPort = 6466)

        assertEquals("roku.local", endpoint.host)
        assertEquals(8060, endpoint.port)
    }

    @Test
    fun blankAddressFails() {
        assertThrows(IllegalArgumentException::class.java) {
            ManualDeviceEndpoint.parse(" ", defaultPort = 8060)
        }
    }
}
