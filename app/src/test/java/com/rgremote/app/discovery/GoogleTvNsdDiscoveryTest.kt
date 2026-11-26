package com.rgremote.app.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleTvNsdDiscoveryTest {
    @Test
    fun serviceType_acceptsAndroidRemoteWithOrWithoutBoundaryDots() {
        assertTrue(isGoogleTvRemoteServiceType("_androidtvremote2._tcp."))
        assertTrue(isGoogleTvRemoteServiceType("._androidtvremote2._tcp."))
        assertTrue(isGoogleTvRemoteServiceType("_ANDROIDTVREMOTE2._TCP"))
    }

    @Test
    fun serviceType_rejectsDifferentServices() {
        assertFalse(isGoogleTvRemoteServiceType("_googlecast._tcp."))
        assertFalse(isGoogleTvRemoteServiceType("_androidtvremote2._udp."))
    }
}
