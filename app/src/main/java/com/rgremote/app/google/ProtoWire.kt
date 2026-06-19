package com.rgremote.app.google

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ProtoWire {
    private const val MAX_FRAME_SIZE_BYTES = 65_536 // 64KB limit

    data class Field(
        val number: Int,
        val wireType: Int,
        val varint: Long? = null,
        val bytes: ByteArray? = null
    )

    fun frame(message: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            writeVarint(message.size.toLong())
            write(message)
        }.toByteArray()

    fun readFrame(input: InputStream): ByteArray {
        val size = readVarint(input).toInt()
        if (size < 0 || size > MAX_FRAME_SIZE_BYTES) {
            throw IOException("ProtoWire frame size limit exceeded: $size bytes")
        }
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(bytes, offset, size - offset)
            if (read < 0) throw EOFException("Connection closed while reading protobuf frame")
            offset += read
        }
        return bytes
    }

    fun parse(bytes: ByteArray): List<Field> {
        val fields = mutableListOf<Field>()
        var index = 0
        while (index < bytes.size) {
            val key = readVarint(bytes, index)
            index = key.nextIndex
            val fieldNumber = (key.value ushr 3).toInt()
            val wireType = (key.value and 0x07).toInt()
            when (wireType) {
                0 -> {
                    val value = readVarint(bytes, index)
                    index = value.nextIndex
                    fields += Field(fieldNumber, wireType, varint = value.value)
                }
                2 -> {
                    val length = readVarint(bytes, index)
                    index = length.nextIndex
                    val end = index + length.value.toInt()
                    if (end > bytes.size) throw EOFException("Length-delimited field overran frame")
                    fields += Field(fieldNumber, wireType, bytes = bytes.copyOfRange(index, end))
                    index = end
                }
                else -> error("Unsupported protobuf wire type $wireType")
            }
        }
        return fields
    }

    fun message(vararg fields: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            fields.forEach { write(it) }
        }.toByteArray()

    fun varintField(number: Int, value: Long): ByteArray =
        ByteArrayOutputStream().apply {
            writeVarint(((number shl 3) or 0).toLong())
            writeVarint(value)
        }.toByteArray()

    fun stringField(number: Int, value: String): ByteArray =
        bytesField(number, value.toByteArray(Charsets.UTF_8))

    fun bytesField(number: Int, bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            writeVarint(((number shl 3) or 2).toLong())
            writeVarint(bytes.size.toLong())
            write(bytes)
        }.toByteArray()

    fun messageField(number: Int, message: ByteArray): ByteArray =
        bytesField(number, message)

    fun OutputStream.writeFrame(message: ByteArray) {
        write(frame(message))
        flush()
    }

    private data class VarintRead(val value: Long, val nextIndex: Int)

    private fun ByteArrayOutputStream.writeVarint(value: Long) {
        var current = value
        while (true) {
            if ((current and 0x7FL.inv()) == 0L) {
                write(current.toInt())
                return
            }
            write(((current and 0x7F) or 0x80).toInt())
            current = current ushr 7
        }
    }

    private fun readVarint(input: InputStream): Long {
        var shift = 0
        var result = 0L
        while (shift < 64) {
            val byte = input.read()
            if (byte < 0) throw EOFException("Connection closed while reading varint")
            result = result or ((byte.toLong() and 0x7F) shl shift)
            if ((byte and 0x80) == 0) return result
            shift += 7
        }
        error("Malformed protobuf varint")
    }

    private fun readVarint(bytes: ByteArray, startIndex: Int): VarintRead {
        var shift = 0
        var result = 0L
        var index = startIndex
        while (shift < 64 && index < bytes.size) {
            val byte = bytes[index].toInt() and 0xFF
            result = result or ((byte.toLong() and 0x7F) shl shift)
            index += 1
            if ((byte and 0x80) == 0) return VarintRead(result, index)
            shift += 7
        }
        error("Malformed protobuf varint")
    }
}
