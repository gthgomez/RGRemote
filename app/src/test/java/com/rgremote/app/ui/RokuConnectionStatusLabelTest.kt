package com.rgremote.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the pure join rule behind [rokuConnectionStatusLabel]. */
class RokuConnectionStatusLabelTest {
    @Test
    fun onlineRoku_includesPowerModeLabel() {
        assertEquals("Online · Standby", rokuConnectionStatusText("Online", "Standby", online = true))
    }

    @Test
    fun offlineRoku_keepsBaseStatusWithoutPowerSuffix() {
        assertEquals("Connection failed", rokuConnectionStatusText("Connection failed", "Standby", online = false))
    }

    @Test
    fun unknownPowerMode_keepsBaseStatus() {
        assertEquals("Online", rokuConnectionStatusText("Online", powerLabel = null, online = true))
    }
}
