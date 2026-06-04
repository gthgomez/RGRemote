package com.rgremote.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RokuXmlReadersDeviceInfoTest {
    @Test
    fun deviceInfoParsesTrueCapabilityFields() {
        val info = RokuXmlReaders.deviceInfo(
            """
            <device-info>
                <friendly-device-name>Living Room Roku</friendly-device-name>
                <serial-number>ABC123</serial-number>
                <power-mode>PowerOn</power-mode>
                <model-name>TCL Roku TV</model-name>
                <model-number>55S451</model-number>
                <network-type>wifi</network-type>
                <is-tv>true</is-tv>
                <supports-tv-power-control>true</supports-tv-power-control>
                <supports-audio-volume-control>true</supports-audio-volume-control>
            </device-info>
            """.trimIndent()
        )

        assertEquals("Living Room Roku", info.friendlyName)
        assertEquals("ABC123", info.serialNumber)
        assertEquals("PowerOn", info.powerMode)
        assertEquals("TCL Roku TV", info.modelName)
        assertEquals("55S451", info.modelNumber)
        assertEquals("wifi", info.networkType)
        assertTrue(info.isTv!!)
        assertTrue(info.supportsTvPowerControl!!)
        assertTrue(info.supportsAudioVolumeControl!!)
    }

    @Test
    fun deviceInfoParsesFalseCapabilityFields() {
        val info = RokuXmlReaders.deviceInfo(
            """
            <device-info>
                <is-tv>false</is-tv>
                <supports-tv-power-control>false</supports-tv-power-control>
                <supports-audio-volume-control>false</supports-audio-volume-control>
            </device-info>
            """.trimIndent()
        )

        assertFalse(info.isTv!!)
        assertFalse(info.supportsTvPowerControl!!)
        assertFalse(info.supportsAudioVolumeControl!!)
    }

    @Test
    fun deviceInfoLeavesMissingCapabilityFieldsUnknown() {
        val info = RokuXmlReaders.deviceInfo(
            """
            <device-info>
                <friendly-device-name>Living Room Roku</friendly-device-name>
            </device-info>
            """.trimIndent()
        )

        assertNull(info.isTv)
        assertNull(info.supportsTvPowerControl)
        assertNull(info.supportsAudioVolumeControl)
    }
}
