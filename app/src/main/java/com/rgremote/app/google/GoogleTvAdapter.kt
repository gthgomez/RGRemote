package com.rgremote.app.google

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteAdapter
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.google.ProtoWire.message
import com.rgremote.app.google.ProtoWire.messageField
import com.rgremote.app.google.ProtoWire.stringField
import com.rgremote.app.google.ProtoWire.varintField
import com.rgremote.app.google.ProtoWire.writeFrame
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleTvAdapter(
    private val registry: DeviceRegistry,
    private val keyStore: GoogleTvKeyStore,
) : RemoteAdapter {
    override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
        runCatching {
            val credential = registry.getCredential(device.id)
                ?: throw IOException("Google TV is not paired yet")
            val serverPin = credential.serverCertificateSha256
                ?: throw IOException("Google TV credential predates certificate pinning; re-pair the device")
            withContext(Dispatchers.IO) {
                val socket = keyStore.pairedSslContext(credential.keyAlias, serverPin)
                    .socketFactory
                    .createSocket(device.ipAddress, device.port) as SSLSocket
                socket.soTimeout = READ_TIMEOUT_MILLIS
                socket.use {
                    it.startHandshake()
                    RemoteV2Session(it).send(command)
                }
            }
        }

    private class RemoteV2Session(private val socket: SSLSocket) {
        private val input = socket.inputStream
        private val output = socket.outputStream
        private var activeFeatures = FEATURE_MASK
        private var sentInitialFallback = false

        fun send(command: RemoteCommand) {
            negotiateUntilStarted()
            requireFeature(command.requiredFeature())
            output.writeFrame(command.toRemoteMessage())
            drainServerMessages()
        }

        private fun negotiateUntilStarted() {
            val deadline = System.currentTimeMillis() + NEGOTIATION_TIMEOUT_MILLIS
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
                        throw IOException("Google TV did not finish Remote v2 negotiation")
                    }
                )
                sawServerMessage = true
                if (started) return
            }
            throw IOException("Google TV Remote v2 negotiation timed out")
        }

        private fun drainServerMessages() {
            val previousTimeout = socket.soTimeout
            socket.soTimeout = POST_COMMAND_DRAIN_MILLIS
            try {
                while (true) {
                    readAndHandleServerMessage(onTimeout = { return })
                }
            } finally {
                socket.soTimeout = previousTimeout
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
                    FIELD_REMOTE_CONFIGURE -> handleConfigure(field)
                    FIELD_REMOTE_SET_ACTIVE -> output.writeFrame(remoteSetActive(activeFeatures))
                    FIELD_REMOTE_PING_REQUEST -> handlePing(field)
                    FIELD_REMOTE_ERROR -> throw IOException("Google TV returned a Remote v2 error")
                    FIELD_REMOTE_START -> if (field.boolField(1) == true) return true
                }
            }
            return false
        }

        private fun handleConfigure(field: ProtoWire.Field) {
            val supportedFeatures = field.nestedFields()
                .firstOrNull { it.number == 1 }
                ?.varint
                ?: FEATURE_MASK
            activeFeatures = FEATURE_MASK and supportedFeatures
            requireFeature(FEATURE_KEY)
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
    }

    companion object {
        internal const val FEATURE_PING = 1L
        internal const val FEATURE_KEY = 2L
        internal const val FEATURE_POWER = 32L
        internal const val FEATURE_VOLUME = 64L
        internal const val FEATURE_APP_LINK = 512L
        internal const val FEATURE_MASK =
            FEATURE_PING or FEATURE_KEY or FEATURE_POWER or FEATURE_VOLUME or FEATURE_APP_LINK

        internal const val FIELD_REMOTE_CONFIGURE = 1
        internal const val FIELD_REMOTE_SET_ACTIVE = 2
        internal const val FIELD_REMOTE_ERROR = 3
        internal const val FIELD_REMOTE_PING_REQUEST = 8
        internal const val FIELD_REMOTE_PING_RESPONSE = 9
        internal const val FIELD_REMOTE_KEY_INJECT = 10
        internal const val FIELD_REMOTE_START = 40
        internal const val FIELD_REMOTE_APP_LINK = 90

        internal const val DIRECTION_SHORT = 3L
        internal const val KEYCODE_HOME = 3L
        internal const val KEYCODE_BACK = 4L
        internal const val KEYCODE_DPAD_UP = 19L
        internal const val KEYCODE_DPAD_DOWN = 20L
        internal const val KEYCODE_DPAD_LEFT = 21L
        internal const val KEYCODE_DPAD_RIGHT = 22L
        internal const val KEYCODE_DPAD_CENTER = 23L
        internal const val KEYCODE_VOLUME_UP = 24L
        internal const val KEYCODE_VOLUME_DOWN = 25L
        internal const val KEYCODE_POWER = 26L
        internal const val KEYCODE_MEDIA_PLAY_PAUSE = 85L
        internal const val KEYCODE_VOLUME_MUTE = 164L
        internal const val KEYCODE_SLEEP = 223L
        internal const val KEYCODE_WAKEUP = 224L

        private const val READ_TIMEOUT_MILLIS = 1_500
        private const val NEGOTIATION_TIMEOUT_MILLIS = 5_000
        private const val POST_COMMAND_DRAIN_MILLIS = 350
    }
}

private fun RemoteCommand.requiredFeature(): Long =
    when (this) {
        is RemoteCommand.Dpad,
        RemoteCommand.Select,
        RemoteCommand.Home,
        RemoteCommand.Back,
        RemoteCommand.PlayPause -> GoogleTvAdapter.FEATURE_KEY
        is RemoteCommand.Volume -> GoogleTvAdapter.FEATURE_VOLUME
        RemoteCommand.PowerOn,
        RemoteCommand.PowerOff,
        RemoteCommand.PowerToggle -> GoogleTvAdapter.FEATURE_POWER
        is RemoteCommand.LaunchApp -> GoogleTvAdapter.FEATURE_APP_LINK
        is RemoteCommand.SetInput -> throw IOException("Google TV cannot switch the TV panel input")
    }

private fun RemoteCommand.toRemoteMessage(): ByteArray =
    when (this) {
        is RemoteCommand.Dpad -> keyInject(direction.keyCode())
        RemoteCommand.Select -> keyInject(GoogleTvAdapter.KEYCODE_DPAD_CENTER)
        RemoteCommand.Home -> keyInject(GoogleTvAdapter.KEYCODE_HOME)
        RemoteCommand.Back -> keyInject(GoogleTvAdapter.KEYCODE_BACK)
        RemoteCommand.PlayPause -> keyInject(GoogleTvAdapter.KEYCODE_MEDIA_PLAY_PAUSE)
        is RemoteCommand.Volume -> keyInject(command.keyCode())
        RemoteCommand.PowerOn -> keyInject(GoogleTvAdapter.KEYCODE_WAKEUP)
        RemoteCommand.PowerOff -> keyInject(GoogleTvAdapter.KEYCODE_SLEEP)
        RemoteCommand.PowerToggle -> keyInject(GoogleTvAdapter.KEYCODE_POWER)
        is RemoteCommand.LaunchApp -> appLink(appIdOrUri.toGoogleTvAppLink())
        is RemoteCommand.SetInput -> throw IOException("Google TV cannot switch the TV panel input")
    }

private fun remoteConfigure(activeFeatures: Long): ByteArray {
    val deviceInfo = message(
        varintField(3, 1),
        stringField(4, "1"),
        stringField(5, "atvremote"),
        stringField(6, "1.0.0"),
    )
    return messageField(
        GoogleTvAdapter.FIELD_REMOTE_CONFIGURE,
        message(
            varintField(1, activeFeatures),
            messageField(2, deviceInfo),
        ),
    ).asRemoteMessage()
}

private fun remoteSetActive(active: Long): ByteArray =
    messageField(GoogleTvAdapter.FIELD_REMOTE_SET_ACTIVE, message(varintField(1, active))).asRemoteMessage()

private fun remotePingResponse(value: Long): ByteArray =
    messageField(GoogleTvAdapter.FIELD_REMOTE_PING_RESPONSE, message(varintField(1, value))).asRemoteMessage()

private fun keyInject(keyCode: Long): ByteArray =
    messageField(
        GoogleTvAdapter.FIELD_REMOTE_KEY_INJECT,
        message(
            varintField(1, keyCode),
            varintField(2, GoogleTvAdapter.DIRECTION_SHORT),
        ),
    ).asRemoteMessage()

private fun appLink(link: String): ByteArray =
    messageField(GoogleTvAdapter.FIELD_REMOTE_APP_LINK, message(stringField(1, link))).asRemoteMessage()

private fun ByteArray.asRemoteMessage(): ByteArray = message(this)

private fun ProtoWire.Field.nestedFields(): List<ProtoWire.Field> =
    bytes?.let { ProtoWire.parse(it) }.orEmpty()

private fun ProtoWire.Field.boolField(number: Int): Boolean? =
    nestedFields().firstOrNull { it.number == number }?.varint?.let { it != 0L }

private fun DpadDirection.keyCode(): Long =
    when (this) {
        DpadDirection.UP -> GoogleTvAdapter.KEYCODE_DPAD_UP
        DpadDirection.DOWN -> GoogleTvAdapter.KEYCODE_DPAD_DOWN
        DpadDirection.LEFT -> GoogleTvAdapter.KEYCODE_DPAD_LEFT
        DpadDirection.RIGHT -> GoogleTvAdapter.KEYCODE_DPAD_RIGHT
    }

private fun VolumeCommand.keyCode(): Long =
    when (this) {
        VolumeCommand.UP -> GoogleTvAdapter.KEYCODE_VOLUME_UP
        VolumeCommand.DOWN -> GoogleTvAdapter.KEYCODE_VOLUME_DOWN
        VolumeCommand.MUTE -> GoogleTvAdapter.KEYCODE_VOLUME_MUTE
    }

private fun String.toGoogleTvAppLink(): String =
    if (contains("://")) this else "market://launch?id=$this"
