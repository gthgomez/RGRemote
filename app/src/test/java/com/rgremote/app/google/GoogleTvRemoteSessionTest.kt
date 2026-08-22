package com.rgremote.app.google

import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private fun testSession(input: ByteArrayInputStream, output: ByteArrayOutputStream): GoogleTvRemoteV2Session =
    GoogleTvRemoteV2Session(
        StreamGoogleTvRemoteTransport(
            input = input,
            output = output,
        ),
    )

private fun negotiationScript(): ByteArrayInputStream =
    framed(
        serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
        serverRemoteSetActive(),
        serverRemoteStart(),
    )

private fun framed(vararg payloads: ByteArray): ByteArrayInputStream =
    ByteArrayInputStream(framedBytes(*payloads))

private fun framedBytes(vararg payloads: ByteArray): ByteArray =
    payloads.fold(byteArrayOf()) { acc, payload ->
        acc + ProtoWire.frame(payload)
    }

/**
 * Serves [script] byte-exactly, then throws [SocketTimeoutException] on every
 * further read, simulating the drain window elapsing on a quiet TLS link.
 */
private class DrainWindowTransport(script: ByteArray) : GoogleTvRemoteTransport {
    override val input: InputStream = object : InputStream() {
        private var index = 0

        override fun read(): Int =
            if (index < script.size) script[index++].toInt() and 0xFF
            else throw SocketTimeoutException("drain window elapsed")

        override fun available(): Int = script.size - index
    }
    override val output = ByteArrayOutputStream()

    override fun pendingBytes(): Int = input.available()

    override fun withDrainReadTimeout(block: () -> Unit) = block()
}

private fun containsKeyCode(output: ByteArray, keyCode: Long): Boolean =
    readClientFrames(output).any { frame ->
        keyCodeFromClientFrame(frame) == keyCode
    }

private fun containsPingResponse(output: ByteArray, pingValue: Long): Boolean =
    readClientFrames(output).any { frame ->
        pingValueFromClientFrame(frame) == pingValue
    }

private fun readClientFrames(output: ByteArray): List<ByteArray> {
    val frames = mutableListOf<ByteArray>()
    val input = ByteArrayInputStream(output)
    while (input.available() > 0) {
        frames += ProtoWire.readFrame(input)
    }
    return frames
}

class GoogleTvRemoteSessionTest {
    @Test
    fun sendHome_injectsHomeKeyCode() {
        val output = ByteArrayOutputStream()
        val session = testSession(
            input = framed(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
                serverRemoteSetActive(),
                serverRemoteStart(),
            ),
            output = output,
        )

        session.send(RemoteCommand.Home)

        assertTrue(containsKeyCode(output.toByteArray(), GoogleTvRemoteProtocol.KEYCODE_HOME))
    }

    @Test
    fun sendDpad_injectsDirectionKeyCode() {
        val output = ByteArrayOutputStream()
        val session = testSession(
            input = negotiationScript(),
            output = output,
        )

        session.send(RemoteCommand.Dpad(DpadDirection.LEFT))

        assertTrue(containsKeyCode(output.toByteArray(), GoogleTvRemoteProtocol.KEYCODE_DPAD_LEFT))
    }

    @Test
    fun sendVolume_injectsVolumeKeyCode() {
        val output = ByteArrayOutputStream()
        val session = testSession(
            input = negotiationScript(),
            output = output,
        )

        session.send(RemoteCommand.Volume(VolumeCommand.UP))

        assertTrue(containsKeyCode(output.toByteArray(), GoogleTvRemoteProtocol.KEYCODE_VOLUME_UP))
    }

    @Test
    fun send_respondsToPingRequest() {
        val output = ByteArrayOutputStream()
        val session = testSession(
            input = framed(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
                serverRemoteSetActive(),
                serverRemoteStart(),
                serverRemotePingRequest(4242),
            ),
            output = output,
        )

        session.send(RemoteCommand.Home)

        assertTrue(containsPingResponse(output.toByteArray(), 4242))
    }

    @Test
    fun send_rejectsRemoteErrorDuringNegotiation() {
        val session = testSession(
            input = framed(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
                serverRemoteError(),
            ),
            output = ByteArrayOutputStream(),
        )

        val error = runCatching { session.send(RemoteCommand.Home) }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertTrue(error?.message?.contains("Remote v2 error") == true)
    }

    @Test
    fun send_rejectsUnsupportedVolumeFeature() {
        val session = testSession(
            input = framed(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_KEY),
                serverRemoteSetActive(GoogleTvRemoteProtocol.FEATURE_KEY),
                serverRemoteStart(),
            ),
            output = ByteArrayOutputStream(),
        )

        val error = runCatching {
            session.send(RemoteCommand.Volume(VolumeCommand.DOWN))
        }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertTrue(error?.message?.contains("feature ${GoogleTvRemoteProtocol.FEATURE_VOLUME}") == true)
    }

    @Test
    fun send_surfacesRemoteErrorAfterNegotiation() {
        val session = testSession(
            input = framed(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
                serverRemoteSetActive(),
                serverRemoteStart(),
                serverRemoteError(),
            ),
            output = ByteArrayOutputStream(),
        )

        val error = runCatching { session.send(RemoteCommand.Home) }.exceptionOrNull()

        assertTrue(error is GoogleTvRemoteServerException)
        assertTrue(error is java.io.IOException)
        assertTrue(error?.message?.contains("Remote v2 error") == true)
    }

    @Test
    fun send_reportsDesyncWhenFrameStraddlesDrainWindow() {
        // Truncated frame: size header + partial payload, then the drain window expires.
        val truncatedPing = ProtoWire.frame(serverRemotePingRequest(9)).copyOfRange(0, 3)
        val script = framedBytes(
            serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
            serverRemoteSetActive(),
            serverRemoteStart(),
        ) + truncatedPing
        val session = GoogleTvRemoteV2Session(DrainWindowTransport(script))

        val error = runCatching { session.send(RemoteCommand.Home) }.exceptionOrNull()

        assertTrue(error is GoogleTvRemoteStreamDesyncException)
        assertTrue(error is java.io.IOException)
    }

    @Test
    fun send_treatsQuietDrainTimeoutAsClean() {
        val transport = DrainWindowTransport(
            framedBytes(
                serverRemoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK),
                serverRemoteSetActive(),
                serverRemoteStart(),
            ),
        )
        val session = GoogleTvRemoteV2Session(transport)

        session.send(RemoteCommand.Home)

        assertTrue(containsKeyCode(transport.output.toByteArray(), GoogleTvRemoteProtocol.KEYCODE_HOME))
    }

    @Test
    fun keyInjectMessage_usesShortDirection() {
        val frame = keyInject(GoogleTvRemoteProtocol.KEYCODE_HOME)
        val nested = ProtoWire.parse(frame)
            .single { it.number == GoogleTvRemoteProtocol.FIELD_REMOTE_KEY_INJECT }
            .nestedFields()

        assertEquals(GoogleTvRemoteProtocol.KEYCODE_HOME, nested.first { it.number == 1 }.varint)
        assertEquals(GoogleTvRemoteProtocol.DIRECTION_SHORT, nested.first { it.number == 2 }.varint)
    }

    @Test
    fun remoteConfigureMessage_includesClientIdentity() {
        val frame = remoteConfigure(GoogleTvRemoteProtocol.FEATURE_MASK)
        val configure = ProtoWire.parse(frame)
            .single { it.number == GoogleTvRemoteProtocol.FIELD_REMOTE_CONFIGURE }
            .nestedFields()
        val deviceInfo = configure.first { it.number == 2 }.nestedFields()

        assertEquals(GoogleTvRemoteProtocol.FEATURE_MASK, configure.first { it.number == 1 }.varint)
        assertEquals("atvremote", deviceInfo.first { it.number == 5 }.bytes?.decodeToString())
    }

    @RunWith(Parameterized::class)
    class KeyCommandTest(
        private val command: RemoteCommand,
        private val expectedKeyCode: Long,
    ) {
        @Test
        fun send_injectsExpectedKeyCode() {
            val output = ByteArrayOutputStream()
            val session = testSession(
                input = negotiationScript(),
                output = output,
            )

            session.send(command)

            assertTrue(containsKeyCode(output.toByteArray(), expectedKeyCode))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun commands(): Collection<Array<Any>> = listOf(
                arrayOf(RemoteCommand.Home, GoogleTvRemoteProtocol.KEYCODE_HOME),
                arrayOf(RemoteCommand.Select, GoogleTvRemoteProtocol.KEYCODE_DPAD_CENTER),
                arrayOf(RemoteCommand.Dpad(DpadDirection.UP), GoogleTvRemoteProtocol.KEYCODE_DPAD_UP),
                arrayOf(RemoteCommand.Dpad(DpadDirection.DOWN), GoogleTvRemoteProtocol.KEYCODE_DPAD_DOWN),
                arrayOf(RemoteCommand.Dpad(DpadDirection.RIGHT), GoogleTvRemoteProtocol.KEYCODE_DPAD_RIGHT),
                arrayOf(
                    RemoteCommand.Volume(VolumeCommand.MUTE),
                    GoogleTvRemoteProtocol.KEYCODE_VOLUME_MUTE,
                ),
            )
        }
    }
}
