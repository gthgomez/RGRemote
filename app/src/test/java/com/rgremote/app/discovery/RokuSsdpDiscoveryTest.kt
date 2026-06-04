package com.rgremote.app.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RokuSsdpDiscoveryTest {
    @Test
    fun defaultTimeoutCoversMxWindowWithBuffer() {
        assertEquals(3_500, ROKU_SSDP_DEFAULT_TIMEOUT_MILLIS)
    }

    @Test
    fun extractsSerialFromRokuUsnWithSearchTargetSuffix() {
        val serial = extractRokuSerialFromUsn("uuid:roku:ecp:YN00AB123456::roku:ecp")

        assertEquals("YN00AB123456", serial)
    }

    @Test
    fun extractsSerialFromMixedCaseRokuUsn() {
        val serial = extractRokuSerialFromUsn("uuid:ROKU:ECP:2N00JH000001::ROKU:ECP")

        assertEquals("2N00JH000001", serial)
    }

    @Test
    fun ignoresRokuUsnWithoutSerialSegment() {
        assertNull(extractRokuSerialFromUsn("uuid:roku:ecp::roku:ecp"))
    }

    @Test
    fun canonicalIdentityUsesSerialForManualCompatibleId() {
        val identity = canonicalRokuIdentity(
            serialNumber = "YN00AB123456",
            ipAddress = "192.168.1.50",
            port = 8060
        )

        assertEquals("roku:yn00ab123456", identity.id)
        assertEquals("YN00AB123456", identity.uniqueId)
    }

    @Test
    fun canonicalIdentityFallsBackToManualEndpointWhenSerialIsMissing() {
        val identity = canonicalRokuIdentity(
            serialNumber = null,
            ipAddress = "192.168.1.50",
            port = 8060
        )

        assertEquals("roku:manual:192.168.1.50:8060", identity.id)
        assertEquals("manual:192.168.1.50:8060", identity.uniqueId)
    }

    @Test
    fun canonicalIdentityPreservesDiscoveredUniqueIdWhenEnrichmentHasNoSerial() {
        val identity = canonicalRokuIdentity(
            serialNumber = null,
            ipAddress = "192.168.1.50",
            port = 8060,
            fallbackUniqueId = "YN00AB123456"
        )

        assertEquals("roku:yn00ab123456", identity.id)
        assertEquals("YN00AB123456", identity.uniqueId)
    }
}
