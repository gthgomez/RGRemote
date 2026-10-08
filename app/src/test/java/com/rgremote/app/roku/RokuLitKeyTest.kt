package com.rgremote.app.roku

import org.junit.Assert.assertEquals
import org.junit.Test

class RokuLitKeyTest {
    @Test
    fun `letters and digits go as-is`() {
        assertEquals("Lit_a", rokuLitKey('a'))
        assertEquals("Lit_Z", rokuLitKey('Z'))
        assertEquals("Lit_5", rokuLitKey('5'))
    }

    @Test
    fun `space is percent-encoded`() {
        assertEquals("Lit_%20", rokuLitKey(' '))
    }

    @Test
    fun `punctuation is percent-encoded`() {
        assertEquals("Lit_%21", rokuLitKey('!'))
        assertEquals("Lit_%2C", rokuLitKey(','))
    }
}
