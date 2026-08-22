package com.rgremote.app.google

import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.google.ProtoWire.writeFrame
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException

internal interface GoogleTvRemoteTransport {
    val input: InputStream
    val output: OutputStream
    fun pendingBytes(): Int
    fun withDrainReadTimeout(block: () -> Unit)
}

internal class SslGoogleTvRemoteTransport(
    private val socket: javax.net.ssl.SSLSocket,
    private val readTimeoutMillis: Int,
    private val drainTimeoutMillis: Int,
) : GoogleTvRemoteTransport {
    override val input: InputStream
        get() = socket.inputStream
    override val output: OutputStream
        get() = socket.outputStream

    override fun pendingBytes(): Int = socket.inputStream.available()

    override fun withDrainReadTimeout(block: () -> Unit) {
        val previous = socket.soTimeout
        socket.soTimeout = drainTimeoutMillis
        try {
            block()
        } finally {
            socket.soTimeout = readTimeoutMillis
        }
    }
}

internal class StreamGoogleTvRemoteTransport(
    override val input: InputStream,
    override val output: OutputStream,
) : GoogleTvRemoteTransport {
    override fun pendingBytes(): Int = input.available()

    override fun withDrainReadTimeout(block: () -> Unit) = block()
}

/** The server explicitly rejected a command (e.g. remote deauthorized); must reach the caller. */
internal class GoogleTvRemoteServerException(message: String) : IOException(message)

/**
 * A timeout or EOF struck after part of a frame was consumed: the prefix is
 * unrecoverable, so framing can no longer be trusted and the session must be
 * recycled. Extends [IOException] so the adapter's stale-session recovery path
 * tears down the connection and reconnects.
 */
internal class GoogleTvRemoteStreamDesyncException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

internal class GoogleTvRemoteV2Session(
    private val transport: GoogleTvRemoteTransport,
    private val negotiationTimeoutMillis: Long = NEGOTIATION_TIMEOUT_MILLIS,
) {
    private val input = transport.input
    private val output = transport.output
    private var activeFeatures = GoogleTvRemoteProtocol.FEATURE_MASK
    private var sentInitialFallback = false
    private var negotiated = false

    fun send(command: RemoteCommand) {
        if (!negotiated) {
            negotiateUntilStarted()
            negotiated = true
        } else {
            drainServerMessages()
        }
        requireFeature(command.requiredFeature())
        output.writeFrame(command.toRemoteMessage())
        drainServerMessages()
    }

    private fun negotiateUntilStarted() {
        val deadline = System.currentTimeMillis() + negotiationTimeoutMillis
        var sawServerMessage = false
        while (System.currentTimeMillis() < deadline) {
            val started = readAndHandleServerMessage(
                onTimeout = {
                    if (!sawServerMessage && !sentInitialFallback) {
                        sentInitialFallback = true
                        output.writeFrame(remoteConfigure(activeFeatures))
                        output.writeFrame(remoteSetActive(activeFeatures))
                        return@readAndHandleServerMessage false
                    }
                    if (System.currentTimeMillis() >= deadline) {
                        throw IOException("Google TV did not finish Remote v2 negotiation (timed out)")
                    }
                    false
                },
            )
            sawServerMessage = true
            if (started) return
        }
        throw IOException("Google TV Remote v2 negotiation timed out")
    }

    private fun drainServerMessages() {
        try {
            while (transport.pendingBytes() > 0) {
                readAndHandleServerMessage(onTimeout = { return })
            }
            transport.withDrainReadTimeout {
                try {
                    readAndHandleServerMessage(onTimeout = { true })
                } catch (_: SocketTimeoutException) {
                    // Expected when no more messages are pending.
                }
            }
        } catch (e: GoogleTvRemoteServerException) {
            // Explicit server rejection must surface as a command failure, not be drained away.
            throw e
        } catch (e: GoogleTvRemoteStreamDesyncException) {
            // Fail the send so the caller recycles this session instead of misframing forever.
            throw e
        } catch (_: IOException) {
            // Stream ended or framing broke; caller will open a fresh session on the next send.
        }
    }

    private inline fun readAndHandleServerMessage(onTimeout: () -> Boolean): Boolean {
        val frame = try {
            readServerFrame()
        } catch (_: SocketTimeoutException) {
            return onTimeout()
        }
        val fields = ProtoWire.parse(frame)
        fields.forEach { field ->
            when (field.number) {
                GoogleTvRemoteProtocol.FIELD_REMOTE_CONFIGURE -> handleConfigure(field)
                GoogleTvRemoteProtocol.FIELD_REMOTE_SET_ACTIVE ->
                    output.writeFrame(remoteSetActive(activeFeatures))
                GoogleTvRemoteProtocol.FIELD_REMOTE_PING_REQUEST -> handlePing(field)
                GoogleTvRemoteProtocol.FIELD_REMOTE_ERROR -> {
                    val message = remoteErrorMessage(field)
                    if (negotiated) {
                        throw GoogleTvRemoteServerException(message)
                    }
                    throw IOException(message)
                }
                GoogleTvRemoteProtocol.FIELD_REMOTE_START ->
                    if (field.boolField(1) == true) return true
            }
        }
        return false
    }

    /**
     * Reads one length-prefixed frame like [ProtoWire.readFrame], but a timeout
     * or EOF after any bytes of the current frame were consumed raises
     * [GoogleTvRemoteStreamDesyncException] instead of silently losing the prefix.
     */
    private fun readServerFrame(): ByteArray {
        val size = readFrameVarint().toInt()
        if (size < 0 || size > MAX_FRAME_SIZE_BYTES) {
            throw IOException("ProtoWire frame size limit exceeded: $size bytes")
        }
        val bytes = ByteArray(size)
        var offset = 0
        try {
            while (offset < size) {
                val read = input.read(bytes, offset, size - offset)
                if (read < 0) throw EOFException("Connection closed mid-frame ($offset/$size bytes)")
                offset += read
            }
        } catch (e: IOException) {
            throw GoogleTvRemoteStreamDesyncException(
                "Google TV Remote v2 stream desynced: partial frame ($offset/$size bytes)",
                e,
            )
        }
        return bytes
    }

    private fun readFrameVarint(): Long {
        var shift = 0
        var result = 0L
        while (shift < 64) {
            val byte = readFrameByte(partialFrame = shift > 0)
            if (byte < 0) throw EOFException("Connection closed while reading protobuf frame")
            result = result or ((byte.toLong() and 0x7F) shl shift)
            if ((byte and 0x80) == 0) return result
            shift += 7
        }
        throw MalformedProtoWireException("Malformed protobuf varint")
    }

    private fun readFrameByte(partialFrame: Boolean): Int =
        try {
            input.read()
        } catch (e: SocketTimeoutException) {
            if (!partialFrame) throw e
            throw GoogleTvRemoteStreamDesyncException(
                "Google TV Remote v2 stream desynced: timeout inside frame header",
                e,
            )
        }

    private fun remoteErrorMessage(field: ProtoWire.Field): String {
        val detail = field.nestedFields().firstOrNull { it.number == 1 }
        val code = detail?.varint
        val text = detail?.bytes?.decodeToString()?.trim()?.takeIf { it.isNotEmpty() }
        return buildString {
            append("Google TV returned a Remote v2 error")
            if (code != null) append(" (code=$code)")
            if (text != null) append(": $text")
        }
    }

    private fun handleConfigure(field: ProtoWire.Field) {
        val supportedFeatures = field.nestedFields()
            .firstOrNull { it.number == 1 }
            ?.varint
            ?: GoogleTvRemoteProtocol.FEATURE_MASK
        activeFeatures = GoogleTvRemoteProtocol.FEATURE_MASK and supportedFeatures
        requireFeature(GoogleTvRemoteProtocol.FEATURE_KEY)
        output.writeFrame(remoteConfigure(activeFeatures))
    }

    private fun handlePing(field: ProtoWire.Field) {
        val pingValue = field.nestedFields()
            .firstOrNull { it.number == 1 }
            ?.varint
            ?: 0L
        output.writeFrame(remotePingResponse(pingValue))
    }

    private fun requireFeature(feature: Long) {
        if ((activeFeatures and feature) != feature) {
            throw IOException("Google TV Remote v2 feature $feature is not available")
        }
    }

    private companion object {
        private const val NEGOTIATION_TIMEOUT_MILLIS = 5_000L

        /** Mirrors the frame size cap enforced by ProtoWire. */
        private const val MAX_FRAME_SIZE_BYTES = 65_536
    }
}
