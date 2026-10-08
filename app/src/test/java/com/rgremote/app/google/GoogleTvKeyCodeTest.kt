package com.rgremote.app.google

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleTvKeyCodeTest {
    @Test
    fun `letters map to android keycodes`() {
        assertEquals(29L, 'a'.googleTvKeyCode())
        assertEquals(54L, 'z'.googleTvKeyCode())
        assertEquals(29L, 'A'.googleTvKeyCode())
    }

    @Test
    fun `digits map to android keycodes`() {
        assertEquals(7L, '0'.googleTvKeyCode())
        assertEquals(16L, '9'.googleTvKeyCode())
    }

    @Test
    fun `common punctuation maps`() {
        assertEquals(62L, ' '.googleTvKeyCode())
        assertEquals(66L, '\n'.googleTvKeyCode())
        assertEquals(67L, '\b'.googleTvKeyCode())
        assertEquals(55L, ','.googleTvKeyCode())
        assertEquals(56L, '.'.googleTvKeyCode())
        assertEquals(69L, '-'.googleTvKeyCode())
        assertEquals(78L, '/'.googleTvKeyCode())
    }

    @Test
    fun `unsupported characters return null`() {
        assertNull('!'.googleTvKeyCode())
        assertNull('?'.googleTvKeyCode())
        assertNull('中'.googleTvKeyCode())
    }

    @Test
    fun `shifted punctuation without unshifted keycode returns null`() {
        assertNull('_'.googleTvKeyCode())
        assertNull('"'.googleTvKeyCode())
        assertNull(':'.googleTvKeyCode())
        assertNull('<'.googleTvKeyCode())
        assertNull('>'.googleTvKeyCode())
    }

    @Test
    fun `symbol keycodes keep their unshifted symbols`() {
        assertEquals(80L, '@'.googleTvKeyCode())
        assertEquals(81L, '+'.googleTvKeyCode())
        assertEquals(82L, '*'.googleTvKeyCode())
        assertEquals(83L, '#'.googleTvKeyCode())
    }
}
