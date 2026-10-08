package com.rgremote.app.roku

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeBroadcastAddressesTest {

    @Test
    fun `ipv4 device address yields global and subnet-directed broadcasts`() {
        assertEquals(
            listOf("255.255.255.255", "10.0.0.255"),
            wakeBroadcastAddresses("10.0.0.81")
        )
    }

    @Test
    fun `null or blank address degrades to global broadcast`() {
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses(null))
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses(""))
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses("  "))
    }

    @Test
    fun `malformed or out-of-range addresses degrade to global broadcast`() {
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses("not-an-ip"))
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses("10.0.0"))
        assertEquals(listOf("255.255.255.255"), wakeBroadcastAddresses("10.0.0.999"))
    }

    @Test
    fun `whitespace around a valid address still resolves the subnet`() {
        assertEquals(
            listOf("255.255.255.255", "192.168.1.255"),
            wakeBroadcastAddresses(" 192.168.1.50 ")
        )
    }
}
