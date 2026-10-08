package com.rgremote.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

class RingHitTestTest {

    private val d = 300f
    private val r = d / 2f
    private val c = r

    @Test
    fun `center maps to select`() {
        assertEquals(RingAction.SELECT, hitTestRing(c, c, d))
    }

    @Test
    fun `inner disc boundary at select fraction maps to select`() {
        // HaloSpec.SelectZoneRadiusFraction = 0.49; just inside must be SELECT.
        val inner = c + r * 0.49f
        assertEquals(RingAction.SELECT, hitTestRing(inner, c, d))
    }

    @Test
    fun `just outside select fraction maps to direction sectors`() {
        val mid = c + r * 0.75f
        assertEquals(RingAction.RIGHT, hitTestRing(mid, c, d))
        assertEquals(RingAction.LEFT, hitTestRing(c - r * 0.75f, c, d))
        assertEquals(RingAction.DOWN, hitTestRing(c, mid, d))
        assertEquals(RingAction.UP, hitTestRing(c, c - r * 0.75f, d))
    }

    @Test
    fun `near edge inside ring still hits sectors`() {
        val near = c + r * 0.99f
        assertEquals(RingAction.RIGHT, hitTestRing(near, c, d))
        assertEquals(RingAction.DOWN, hitTestRing(c, near, d))
    }

    @Test
    fun `diagonal points split by dominant axis`() {
        val v = r * 0.75f
        val diag = v / sqrt(2f)
        // |dx| == |dy| resolves as horizontal (dx >= dy tie-break).
        assertEquals(RingAction.RIGHT, hitTestRing(c + diag, c + diag, d))
        assertEquals(RingAction.LEFT, hitTestRing(c - diag, c + diag, d))
        val steeperX = r * 0.4f
        val steeperY = r * 0.7f
        assertEquals(RingAction.DOWN, hitTestRing(c + steeperX, c + steeperY, d))
        assertEquals(RingAction.UP, hitTestRing(c + steeperX, c - steeperY, d))
    }

    @Test
    fun `outside the outer radius misses`() {
        assertNull(hitTestRing(c + r * 1.01f, c, d))
        assertNull(hitTestRing(c + r * 0.75f, c + r * 0.75f, d)) // hypot > r
        assertNull(hitTestRing(-1f, c, d))
        assertNull(hitTestRing(c, d + 5f, d))
    }

    @Test
    fun `degenerate diameter misses`() {
        assertNull(hitTestRing(0f, 0f, 0f))
        assertNull(hitTestRing(0f, 0f, -10f))
    }
}
