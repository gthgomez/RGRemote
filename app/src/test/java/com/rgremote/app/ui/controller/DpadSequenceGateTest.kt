package com.rgremote.app.ui.controller

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpadSequenceGateTest {

    @Test
    fun `token issued by begin is valid`() {
        val gate = DpadSequenceGate()
        val token = gate.beginSequence()
        assertTrue(gate.isValid(token))
    }

    @Test
    fun `cancel invalidates the active token`() {
        val gate = DpadSequenceGate()
        val token = gate.beginSequence()
        gate.cancelActiveSequence()
        assertFalse(gate.isValid(token))
    }

    @Test
    fun `a new begin invalidates earlier sequences`() {
        val gate = DpadSequenceGate()
        val first = gate.beginSequence()
        val second = gate.beginSequence()
        assertFalse(gate.isValid(first))
        assertTrue(gate.isValid(second))
        assertNotEquals(first, second)
    }

    @Test
    fun `tokens issued after cancel are valid again`() {
        val gate = DpadSequenceGate()
        val stale = gate.beginSequence()
        gate.cancelActiveSequence()
        val fresh = gate.beginSequence()
        assertFalse(gate.isValid(stale))
        assertTrue(gate.isValid(fresh))
    }

    @Test
    fun `cancel without begin still invalidates nothing but bumps generation`() {
        val gate = DpadSequenceGate()
        val before = gate.cancelActiveSequence()
        val token = gate.beginSequence()
        assertTrue(gate.isValid(token))
        assertNotEquals(before, token)
    }
}
