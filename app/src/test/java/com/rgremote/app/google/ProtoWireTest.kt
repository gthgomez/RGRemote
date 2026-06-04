package com.rgremote.app.google

import com.rgremote.app.google.ProtoWire.writeFrame
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtoWireTest {
    @Test
    fun frameRoundTripPreservesMessage() {
        val message = ProtoWire.message(
            ProtoWire.varintField(1, 512),
            ProtoWire.stringField(2, "RGRemote")
        )

        assertArrayEquals(message, ProtoWire.readFrame(ByteArrayInputStream(ProtoWire.frame(message))))
    }

    @Test
    fun parseNestedLengthDelimitedMessage() {
        val nested = ProtoWire.message(ProtoWire.varintField(1, 42))
        val outer = ProtoWire.message(ProtoWire.messageField(8, nested))

        val field = ProtoWire.parse(outer).single()
        val nestedField = ProtoWire.parse(field.bytes!!).single()

        assertEquals(8, field.number)
        assertEquals(42L, nestedField.varint)
    }

    @Test
    fun outputStreamWriteFramePrefixesLength() {
        val output = ByteArrayOutputStream()
        val payload = byteArrayOf(1, 2, 3)

        output.writeFrame(payload)

        assertArrayEquals(payload, ProtoWire.readFrame(ByteArrayInputStream(output.toByteArray())))
    }
}
