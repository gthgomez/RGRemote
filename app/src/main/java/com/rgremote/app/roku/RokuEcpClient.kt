package com.rgremote.app.roku

import com.rgremote.app.domain.ActiveApp
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteAdapter
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo
import com.rgremote.app.domain.VolumeCommand
import com.rgremote.app.network.RokuXmlReaders
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RokuNetworkAccessException(
    path: String,
    code: Int
) : IOException(
    "Roku refused /$path with HTTP $code. Set Settings > System > Advanced system settings > Control by mobile apps > Network access to Enabled."
)

class RokuEcpClient(
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    private val wakeTimeoutMillis: Int = WAKE_TIMEOUT_MILLIS,
) : RemoteAdapter, RokuEcpGateway {
    override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
        runCatching {
            when (command) {
                is RemoteCommand.Dpad -> post(device, "keypress/${command.direction.rokuKey()}")
                RemoteCommand.Select -> post(device, "keypress/Select")
                RemoteCommand.Home -> post(device, "keypress/Home")
                RemoteCommand.Back -> post(device, "keypress/Back")
                RemoteCommand.PlayPause -> post(device, "keypress/Play")
                is RemoteCommand.Volume -> post(device, "keypress/${command.command.rokuKey()}")
                RemoteCommand.PowerOn -> post(device, "keypress/Home", wakeTimeoutMillis)
                RemoteCommand.PowerOff -> post(device, "keypress/PowerOff")
                RemoteCommand.PowerToggle -> throw IOException("Roku ECP does not provide a reliable power toggle; use Power Off or Wake/Home")
                is RemoteCommand.LaunchApp -> post(device, RokuLaunchTarget.parse(command.appIdOrUri).path)
                is RemoteCommand.SetInput -> post(device, "keypress/${command.port.rokuKey}")
            }
        }

    override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
        RokuXmlReaders.deviceInfo(get(device, "query/device-info"))

    override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? =
        RokuXmlReaders.activeApp(get(device, "query/active-app"))

    override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> =
        RokuXmlReaders.apps(get(device, "query/apps"))

    private suspend fun get(device: RegisteredDevice, path: String): String =
        withContext(Dispatchers.IO) {
            open(device, path, method = "GET", timeoutMillis).use { connection ->
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw rokuHttpException(path, code, method = "GET")
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        }

    private suspend fun post(
        device: RegisteredDevice,
        path: String,
        requestTimeoutMillis: Int = timeoutMillis,
    ) {
        withContext(Dispatchers.IO) {
            open(device, path, method = "POST", requestTimeoutMillis).use { connection ->
                connection.doOutput = true
                connection.outputStream.use { it.write(ByteArray(0)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw rokuHttpException(path, code, method = "POST")
                }
            }
        }
    }

    private fun open(
        device: RegisteredDevice,
        path: String,
        method: String,
        requestTimeoutMillis: Int = timeoutMillis,
    ): HttpURLConnection {
        val url = URL("http://${device.ipAddress}:${device.port}/$path")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = requestTimeoutMillis
            readTimeout = requestTimeoutMillis
            useCaches = false
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 2_000
        const val WAKE_TIMEOUT_MILLIS = 6_000
    }

    private fun rokuHttpException(path: String, code: Int, method: String): IOException =
        if (code == HttpURLConnection.HTTP_FORBIDDEN) {
            RokuNetworkAccessException(path, code)
        } else {
            IOException("Roku $method /$path failed with HTTP $code")
        }
}

private fun DpadDirection.rokuKey(): String =
    when (this) {
        DpadDirection.UP -> "Up"
        DpadDirection.DOWN -> "Down"
        DpadDirection.LEFT -> "Left"
        DpadDirection.RIGHT -> "Right"
    }

private fun VolumeCommand.rokuKey(): String =
    when (this) {
        VolumeCommand.UP -> "VolumeUp"
        VolumeCommand.DOWN -> "VolumeDown"
        VolumeCommand.MUTE -> "VolumeMute"
    }

private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        disconnect()
    }
