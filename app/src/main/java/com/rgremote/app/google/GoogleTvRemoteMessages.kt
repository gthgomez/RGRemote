package com.rgremote.app.google

import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.google.ProtoWire.message
import com.rgremote.app.google.ProtoWire.messageField
import com.rgremote.app.google.ProtoWire.stringField
import com.rgremote.app.google.ProtoWire.varintField
import java.io.IOException

internal object GoogleTvRemoteProtocol {
    const val FEATURE_PING = 1L
    const val FEATURE_KEY = 2L
    const val FEATURE_POWER = 32L
    const val FEATURE_VOLUME = 64L
    const val FEATURE_APP_LINK = 512L
    const val FEATURE_MASK =
        FEATURE_PING or FEATURE_KEY or FEATURE_POWER or FEATURE_VOLUME or FEATURE_APP_LINK

    const val FIELD_REMOTE_CONFIGURE = 1
    const val FIELD_REMOTE_SET_ACTIVE = 2
    const val FIELD_REMOTE_ERROR = 3
    const val FIELD_REMOTE_PING_REQUEST = 8
    const val FIELD_REMOTE_PING_RESPONSE = 9
    const val FIELD_REMOTE_KEY_INJECT = 10
    const val FIELD_REMOTE_START = 40
    const val FIELD_REMOTE_APP_LINK = 90

    const val DIRECTION_SHORT = 3L
    const val KEYCODE_HOME = 3L
    const val KEYCODE_BACK = 4L
    const val KEYCODE_DPAD_UP = 19L
    const val KEYCODE_DPAD_DOWN = 20L
    const val KEYCODE_DPAD_LEFT = 21L
    const val KEYCODE_DPAD_RIGHT = 22L
    const val KEYCODE_DPAD_CENTER = 23L
    const val KEYCODE_VOLUME_UP = 24L
    const val KEYCODE_VOLUME_DOWN = 25L
    const val KEYCODE_POWER = 26L
    const val KEYCODE_MEDIA_PLAY_PAUSE = 85L
    const val KEYCODE_VOLUME_MUTE = 164L
    const val KEYCODE_SLEEP = 223L
    const val KEYCODE_WAKEUP = 224L
}

internal fun RemoteCommand.requiredFeature(): Long =
    when (this) {
        is RemoteCommand.Dpad,
        RemoteCommand.Select,
        RemoteCommand.Home,
        RemoteCommand.Back,
        RemoteCommand.PlayPause -> GoogleTvRemoteProtocol.FEATURE_KEY
        is RemoteCommand.Volume -> GoogleTvRemoteProtocol.FEATURE_VOLUME
        RemoteCommand.PowerOn,
        RemoteCommand.PowerOff,
        RemoteCommand.PowerToggle -> GoogleTvRemoteProtocol.FEATURE_POWER
        is RemoteCommand.LaunchApp -> GoogleTvRemoteProtocol.FEATURE_APP_LINK
        is RemoteCommand.SetInput -> throw IOException("Google TV cannot switch the TV panel input")
    }

internal fun RemoteCommand.toRemoteMessage(): ByteArray =
    when (this) {
        is RemoteCommand.Dpad -> keyInject(direction.keyCode())
        RemoteCommand.Select -> keyInject(GoogleTvRemoteProtocol.KEYCODE_DPAD_CENTER)
        RemoteCommand.Home -> keyInject(GoogleTvRemoteProtocol.KEYCODE_HOME)
        RemoteCommand.Back -> keyInject(GoogleTvRemoteProtocol.KEYCODE_BACK)
        RemoteCommand.PlayPause -> keyInject(GoogleTvRemoteProtocol.KEYCODE_MEDIA_PLAY_PAUSE)
        is RemoteCommand.Volume -> keyInject(command.keyCode())
        RemoteCommand.PowerOn -> keyInject(GoogleTvRemoteProtocol.KEYCODE_WAKEUP)
        RemoteCommand.PowerOff -> keyInject(GoogleTvRemoteProtocol.KEYCODE_SLEEP)
        RemoteCommand.PowerToggle -> keyInject(GoogleTvRemoteProtocol.KEYCODE_POWER)
        is RemoteCommand.LaunchApp -> appLink(appIdOrUri.toGoogleTvAppLink())
        is RemoteCommand.SetInput -> throw IOException("Google TV cannot switch the TV panel input")
    }

internal fun remoteConfigure(activeFeatures: Long): ByteArray {
    val deviceInfo = message(
        varintField(3, 1),
        stringField(4, "1"),
        stringField(5, "atvremote"),
        stringField(6, "1.0.0"),
    )
    return messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_CONFIGURE,
        message(
            varintField(1, activeFeatures),
            messageField(2, deviceInfo),
        ),
    ).asRemoteMessage()
}

internal fun remoteSetActive(active: Long): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_SET_ACTIVE,
        message(varintField(1, active)),
    ).asRemoteMessage()

internal fun remotePingResponse(value: Long): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_PING_RESPONSE,
        message(varintField(1, value)),
    ).asRemoteMessage()

internal fun keyInject(keyCode: Long): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_KEY_INJECT,
        message(
            varintField(1, keyCode),
            varintField(2, GoogleTvRemoteProtocol.DIRECTION_SHORT),
        ),
    ).asRemoteMessage()

internal fun appLink(link: String): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_APP_LINK,
        message(stringField(1, link)),
    ).asRemoteMessage()

internal fun serverRemoteConfigure(supportedFeatures: Long): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_CONFIGURE,
        message(varintField(1, supportedFeatures)),
    )

internal fun serverRemoteSetActive(active: Long = GoogleTvRemoteProtocol.FEATURE_MASK): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_SET_ACTIVE,
        message(varintField(1, active)),
    )

internal fun serverRemoteStart(): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_START,
        message(varintField(1, 1)),
    )

internal fun serverRemotePingRequest(value: Long): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_PING_REQUEST,
        message(varintField(1, value)),
    )

internal fun serverRemoteError(): ByteArray =
    messageField(
        GoogleTvRemoteProtocol.FIELD_REMOTE_ERROR,
        message(varintField(1, 1)),
    )

private fun ByteArray.asRemoteMessage(): ByteArray = message(this)

private fun DpadDirection.keyCode(): Long =
    when (this) {
        DpadDirection.UP -> GoogleTvRemoteProtocol.KEYCODE_DPAD_UP
        DpadDirection.DOWN -> GoogleTvRemoteProtocol.KEYCODE_DPAD_DOWN
        DpadDirection.LEFT -> GoogleTvRemoteProtocol.KEYCODE_DPAD_LEFT
        DpadDirection.RIGHT -> GoogleTvRemoteProtocol.KEYCODE_DPAD_RIGHT
    }

private fun VolumeCommand.keyCode(): Long =
    when (this) {
        VolumeCommand.UP -> GoogleTvRemoteProtocol.KEYCODE_VOLUME_UP
        VolumeCommand.DOWN -> GoogleTvRemoteProtocol.KEYCODE_VOLUME_DOWN
        VolumeCommand.MUTE -> GoogleTvRemoteProtocol.KEYCODE_VOLUME_MUTE
    }

private fun String.toGoogleTvAppLink(): String =
    if (contains("://")) this else "market://launch?id=$this"

internal fun ProtoWire.Field.nestedFields(): List<ProtoWire.Field> =
    bytes?.let { ProtoWire.parse(it) }.orEmpty()

internal fun ProtoWire.Field.boolField(number: Int): Boolean? =
    nestedFields().firstOrNull { it.number == number }?.varint?.let { it != 0L }

internal fun keyCodeFromClientFrame(frame: ByteArray): Long? {
    val outer = ProtoWire.parse(frame)
    val inject = outer.firstOrNull { it.number == GoogleTvRemoteProtocol.FIELD_REMOTE_KEY_INJECT }
        ?: return null
    return inject.nestedFields().firstOrNull { it.number == 1 }?.varint
}

internal fun pingValueFromClientFrame(frame: ByteArray): Long? {
    val outer = ProtoWire.parse(frame)
    val response = outer.firstOrNull { it.number == GoogleTvRemoteProtocol.FIELD_REMOTE_PING_RESPONSE }
        ?: return null
    return response.nestedFields().firstOrNull { it.number == 1 }?.varint
}
