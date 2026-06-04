package com.rgremote.app.roku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RokuPowerModeTest {
    @Test
    fun displayLabel_mapsKnownPowerModes() {
        assertEquals("On", RokuPowerMode.displayLabel("PowerOn"))
        assertEquals("Off", RokuPowerMode.displayLabel("PowerOff"))
        assertEquals("Standby", RokuPowerMode.displayLabel("DisplayOff"))
        assertEquals("Standby", RokuPowerMode.displayLabel("standby"))
    }

    @Test
    fun displayLabel_returnsNullForBlank() {
        assertNull(RokuPowerMode.displayLabel(null))
        assertNull(RokuPowerMode.displayLabel("   "))
    }

    @Test
    fun isLikelyReachable_falseWhenFullyOff() {
        assertFalse(RokuPowerMode.isLikelyReachable("PowerOff"))
    }

    @Test
    fun isLikelyReachable_trueForOnStandbyOrUnknown() {
        assertTrue(RokuPowerMode.isLikelyReachable("PowerOn"))
        assertTrue(RokuPowerMode.isLikelyReachable("DisplayOff"))
        assertTrue(RokuPowerMode.isLikelyReachable(null))
    }

    @Test
    fun wakeHint_explainsOffAndStandby() {
        assertTrue(
            RokuPowerMode.wakeHint("PowerOff")!!.contains("Fast TV Start")
        )
        assertTrue(
            RokuPowerMode.wakeHint("DisplayOff")!!.contains("standby")
        )
        assertNull(RokuPowerMode.wakeHint("PowerOn"))
    }
}
