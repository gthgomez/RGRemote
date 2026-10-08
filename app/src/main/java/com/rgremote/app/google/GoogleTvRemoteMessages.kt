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
    const val KEYCODE_MEDIA_REWIND = 89L
    const val KEYCODE_MEDIA_FAST_FORWARD = 90L
    const val KEYCODE_VOLUME_MUTE = 164L
    const val KEYCODE_SLEEP = 223L
    const val KEYCODE_WAKEUP = 224L

    const val KEYCODE_SPACE = 62L
    const val KEYCODE_ENTER = 66L
    const val KEYCODE_DEL = 67L
    const val KEYCODE_COMMA = 55L
    const val KEYCODE_PERIOD = 56L
    const val KEYCODE_GRAVE = 68L
    const val KEYCODE_MINUS = 69L
    const val KEYCODE_EQUALS = 70L
    const val KEYCODE_LEFT_BRACKET = 73L
    const val KEYCODE_RIGHT_BRACKET = 74L
    const val KEYCODE_BACKSLASH = 75L
    const val KEYCODE_SEMICOLON = 76L
    const val KEYCODE_APOSTROPHE = 77L
    const val KEYCODE_SLASH = 78L
    const val KEYCODE_AT = 80L
    const val KEYCODE_PLUS = 81L
    const val KEYCODE_STAR = 82L
    const val KEYCODE_POUND = 83L
}

/**
 * Android keycode for a character, or null when the protocol path here cannot
 * express it: uppercase collapses to its lowercase letter, and shifted
 * punctuation (no dedicated unshifted keycode) is skipped — key injection
 * carries no shift meta, so e.g. '_' would silently arrive as '-'.
 * KEYCODE_PLUS/AT/STAR/POUND are excluded from that rule: Android defines them
 * as symbol keycodes whose unshifted mapping is the symbol itself.
 */
internal fun Char.googleTvKeyCode(): Long? =
    when (this) {
        in 'a'..'z' -> (this - 'a' + 29L)
        in 'A'..'Z' -> (this - 'A' + 29L)
        in '0'..'9' -> (this - '0' + 7L)
        ' ' -> GoogleTvRemoteProtocol.KEYCODE_SPACE
        '\n' -> GoogleTvRemoteProtocol.KEYCODE_ENTER
        '\b' -> GoogleTvRemoteProtocol.KEYCODE_DEL
        '.' -> GoogleTvRemoteProtocol.KEYCODE_PERIOD
        ',' -> GoogleTvRemoteProtocol.KEYCODE_COMMA
        '-' -> GoogleTvRemoteProtocol.KEYCODE_MINUS
        '=' -> GoogleTvRemoteProtocol.KEYCODE_EQUALS
        '+' -> GoogleTvRemoteProtocol.KEYCODE_PLUS
        '@' -> GoogleTvRemoteProtocol.KEYCODE_AT
        '*' -> GoogleTvRemoteProtocol.KEYCODE_STAR
        '#' -> GoogleTvRemoteProtocol.KEYCODE_POUND
        '/' -> GoogleTvRemoteProtocol.KEYCODE_SLASH
        '\\' -> GoogleTvRemoteProtocol.KEYCODE_BACKSLASH
        ';' -> GoogleTvRemoteProtocol.KEYCODE_SEMICOLON
        '\'' -> GoogleTvRemoteProtocol.KEYCODE_APOSTROPHE
        '`' -> GoogleTvRemoteProtocol.KEYCODE_GRAVE
        '[' -> GoogleTvRemoteProtocol.KEYCODE_LEFT_BRACKET
        ']' -> GoogleTvRemoteProtocol.KEYCODE_RIGHT_BRACKET
        else -> null
    }

internal fun RemoteCommand.requiredFeature(): Long =
    when (this) {
        is RemoteCommand.Dpad,
        RemoteCommand.Select,
        RemoteCommand.Home,
        RemoteCommand.Back,
        RemoteCommand.PlayPause,
        RemoteCommand.Rewind,
        RemoteCommand.FastForward,
        is RemoteCommand.Character,
        RemoteCommand.KeyboardEnter,
        RemoteCommand.KeyboardBackspace -> GoogleTvRemoteProtocol.FEATURE_KEY
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
        RemoteCommand.Rewind -> keyInject(GoogleTvRemoteProtocol.KEYCODE_MEDIA_REWIND)
        RemoteCommand.FastForward -> keyInject(GoogleTvRemoteProtocol.KEYCODE_MEDIA_FAST_FORWARD)
        is RemoteCommand.Character -> keyInject(
            char.googleTvKeyCode()
                ?: throw IOException("Character '${char}' cannot be typed on Google TV")
        )
        RemoteCommand.KeyboardEnter -> keyInject(GoogleTvRemoteProtocol.KEYCODE_ENTER)
        RemoteCommand.KeyboardBackspace -> keyInject(GoogleTvRemoteProtocol.KEYCODE_DEL)
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
