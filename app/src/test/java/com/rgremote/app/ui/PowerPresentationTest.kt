package com.rgremote.app.ui

import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerPresentationTest {

    @Test
    fun `google tv always uses power toggle`() {
        val p = resolvePowerPresentation(DeviceType.GOOGLE_TV, null, ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerToggle, p.command)
        assertFalse(p.isWake)
    }

    @Test
    fun `roku online with power mode on powers off`() {
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "On", ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerOff, p.command)
        assertFalse(p.isWake)
    }

    @Test
    fun `roku standby wakes`() {
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "Standby", ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
    }

    @Test
    fun `roku offline treated as standby wakes`() {
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "On", ConnectionStatus.OFFLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
    }

    @Test
    fun `roku deep off wakes instead of powering off`() {
        // Regression: ECP powerMode "poweroff" (deep off / Fast TV Start) must
        // resolve to wake — the old logic sent PowerOff at a TV that needed waking.
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "poweroff", ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
    }

    @Test
    fun `roku deep off while offline wakes`() {
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "poweroff", ConnectionStatus.OFFLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
    }

    @Test
    fun `roku displayoff wakes`() {
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, "displayoff", ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
    }

    @Test
    fun `roku unknown power mode never sends power toggle`() {
        // Regression: RokuEcpClient rejects PowerToggle; unknown mode must wake instead.
        val p = resolvePowerPresentation(DeviceType.ROKU_TV, null, ConnectionStatus.ONLINE)
        assertEquals(RemoteCommand.PowerOn, p.command)
        assertTrue(p.isWake)
        assertTrue(p.isUncertain)
    }
}
