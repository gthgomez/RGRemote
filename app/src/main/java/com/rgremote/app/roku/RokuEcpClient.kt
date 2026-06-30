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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
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
                RemoteCommand.PowerOn -> {
                    device.wifiMac?.let { sendWakeOnLan(it) }
                    device.ethernetMac?.let { sendWakeOnLan(it) }
                    post(device, "keypress/Home", wakeTimeoutMillis)
                }
                RemoteCommand.PowerOff -> post(device, "keypress/PowerOff")
                RemoteCommand.PowerToggle -> throw IOException("Roku ECP does not provide a reliable power toggle; use Power Off or Wake/Home")
                is RemoteCommand.LaunchApp -> post(device, RokuLaunchTarget.parse(command.appIdOrUri).path)
                is RemoteCommand.SetInput -> post(device, "keypress/${command.port.rokuKey}")
            }
        }

    private fun sendWakeOnLan(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").replace("-", "")
        if (cleanMac.length != 12) return
        val macBytes = ByteArray(6)
        for (i in 0 until 6) {
            macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        val payload = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) {
            payload[i] = 0xFF.toByte()
        }
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, payload, 6 + i * 6, 6)
        }
        try {
            val address = InetAddress.getByName("255.255.255.255")
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(payload, payload.size, address, 9))
                socket.send(DatagramPacket(payload, payload.size, address, 7))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    // Consume error body so connection can return to keep-alive pool
                    try {
                        connection.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) { }
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
                    // Consume error body so connection can return to keep-alive pool
                    try {
                        connection.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) { }
                    throw rokuHttpException(path, code, method = "POST")
                }
                // Consuming the input stream returns the connection to the keep-alive pool.
                try {
                    connection.inputStream.use { it.readBytes() }
                } catch (e: Exception) {
                    // Drain error stream as best-effort, then propagate the failure.
                    try {
                        connection.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) {}
                    throw IOException("Roku POST /$path: failed to read response body", e)
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

// Do NOT call disconnect() on success. Returning without disconnect() lets the JVM
// keep the socket alive in HttpURLConnection's internal connection pool, enabling
// HTTP Keep-Alive reuse across rapid ECP button presses. Each caller already
// closes its response stream (inputStream / outputStream) via stdlib `.use {}`.
//
// If [block] throws, disconnect() IS called to prevent a stale/undrained connection
// from being returned to the pool.
private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } catch (e: Exception) {
        disconnect()
        throw e
    }
