package com.rgremote.app.google

import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.google.ProtoWire.writeFrame
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
        } catch (_: IOException) {
            // Stream ended or framing broke; caller will open a fresh session on the next send.
        }
    }

    private inline fun readAndHandleServerMessage(onTimeout: () -> Boolean): Boolean {
        val frame = try {
            ProtoWire.readFrame(input)
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
                GoogleTvRemoteProtocol.FIELD_REMOTE_ERROR ->
                    throw IOException("Google TV returned a Remote v2 error")
                GoogleTvRemoteProtocol.FIELD_REMOTE_START ->
                    if (field.boolField(1) == true) return true
            }
        }
        return false
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
    }
}
