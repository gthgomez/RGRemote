package com.rgremote.app.roku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RokuLaunchTargetTest {
    @Test
    fun appIdBuildsPlainLaunchPath() {
        assertEquals("launch/8378", RokuLaunchTarget.parse("8378").path)
    }

    @Test
    fun launchPrefixIsAccepted() {
        assertEquals("launch/8378", RokuLaunchTarget.parse("launch/8378").path)
    }

    @Test
    fun fullEcpUrlIsAccepted() {
        val target = RokuLaunchTarget.parse("http://192.168.1.20:8060/launch/8378?contentId=my%20movie&mediaType=movie")

        assertEquals("launch/8378?contentId=my%20movie&mediaType=movie", target.path)
    }

    @Test
    fun queryParametersAreEncodedWithoutEncodingSeparators() {
        val target = RokuLaunchTarget.parse("8378?contentId=season 1|episode 2&mediaType=series")

        assertEquals("launch/8378?contentId=season%201%7Cepisode%202&mediaType=series", target.path)
    }

    @Test
    fun blankTargetFails() {
        assertThrows(IllegalArgumentException::class.java) {
            RokuLaunchTarget.parse("   ")
        }
    }
}
