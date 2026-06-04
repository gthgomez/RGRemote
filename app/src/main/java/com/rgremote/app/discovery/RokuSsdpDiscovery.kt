package com.rgremote.app.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val ROKU_SSDP_MX_SECONDS = 3
internal const val ROKU_SSDP_DEFAULT_TIMEOUT_MILLIS = ROKU_SSDP_MX_SECONDS * 1_000 + 500

private const val ROKU_SSDP_PROBE_COUNT = 3
private const val ROKU_SSDP_PROBE_SPACING_MILLIS = 100L

class RokuSsdpDiscovery(
    context: Context,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext = context.applicationContext

    suspend fun scan(timeoutMillis: Int = ROKU_SSDP_DEFAULT_TIMEOUT_MILLIS): List<RegisteredDevice> =
        withContext(Dispatchers.IO) {
            val lock = multicastLock()
            lock?.acquire()
            try {
                DatagramSocket().use { socket ->
                    val deadlineMillis = clockMillis() + timeoutMillis.coerceAtLeast(0)
                    val search = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("Host: 239.255.255.250:1900\r\n")
                        append("Man: \"ssdp:discover\"\r\n")
                        append("ST: roku:ecp\r\n")
                        append("MX: $ROKU_SSDP_MX_SECONDS\r\n")
                        append("\r\n")
                    }.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(
                        search,
                        search.size,
                        InetAddress.getByName("239.255.255.250"),
                        1900
                    )
                    sendSearchProbes(socket, packet, deadlineMillis)
                    receiveResponses(socket, deadlineMillis)
                }
            } finally {
                if (lock?.isHeld == true) lock.release()
            }
        }

    private fun sendSearchProbes(
        socket: DatagramSocket,
        packet: DatagramPacket,
        deadlineMillis: Long
    ) {
        repeat(ROKU_SSDP_PROBE_COUNT) { index ->
            if (clockMillis() >= deadlineMillis) return
            socket.send(packet)
            if (index < ROKU_SSDP_PROBE_COUNT - 1) {
                sleepUntilNextProbe(deadlineMillis)
            }
        }
    }

    private fun sleepUntilNextProbe(deadlineMillis: Long) {
        val sleepMillis = minOf(ROKU_SSDP_PROBE_SPACING_MILLIS, deadlineMillis - clockMillis())
        if (sleepMillis <= 0) return
        try {
            Thread.sleep(sleepMillis)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun receiveResponses(socket: DatagramSocket, deadlineMillis: Long): List<RegisteredDevice> {
        val devices = linkedMapOf<String, RegisteredDevice>()
        while (true) {
            val remainingMillis = remainingMillis(deadlineMillis)
            if (remainingMillis <= 0) break
            socket.soTimeout = remainingMillis
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)
            val received = runCatching {
                socket.receive(packet)
                true
            }.getOrDefault(false)
            if (!received) break

            val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val headers = response.headers()
            if (!headers["st"].equals("roku:ecp", ignoreCase = true) &&
                !headers["usn"].orEmpty().contains("roku:ecp", ignoreCase = true)
            ) {
                continue
            }
            val location = headers["location"] ?: continue
            val uri = runCatching { URI(location) }.getOrNull() ?: continue
            val ip = uri.host ?: packet.address.hostAddress ?: continue
            val port = if (uri.port > 0) uri.port else 8060
            val identity = canonicalRokuIdentity(
                serialNumber = extractRokuSerialFromUsn(headers["usn"]),
                ipAddress = ip,
                port = port
            )
            devices[identity.id] = RegisteredDevice(
                id = identity.id,
                type = DeviceType.ROKU_TV,
                ipAddress = ip,
                port = port,
                uniqueId = identity.uniqueId,
                friendlyName = "Roku TV $ip",
                lastSeenMillis = clockMillis(),
                hdmiPortMapping = null,
                isOnline = true,
                consecutiveFailures = 0
            )
        }
        return devices.values.toList()
    }

    private fun multicastLock(): WifiManager.MulticastLock? {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        return wifi.createMulticastLock("RGRemote:RokuSsdp").apply {
            setReferenceCounted(false)
        }
    }

    private fun remainingMillis(deadlineMillis: Long): Int =
        (deadlineMillis - clockMillis())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}

private fun String.headers(): Map<String, String> =
    lineSequence()
        .mapNotNull { line ->
            val index = line.indexOf(':')
            if (index <= 0) null else {
                line.substring(0, index).trim().lowercase(Locale.US) to
                    line.substring(index + 1).trim()
            }
        }
        .toMap()

internal data class RokuDiscoveryIdentity(
    val id: String,
    val uniqueId: String
)

internal fun extractRokuSerialFromUsn(usn: String?): String? {
    val primaryUsn = usn?.substringBefore("::")?.trim().orEmpty()
    if (primaryUsn.isEmpty()) return null

    val segments = primaryUsn
        .split(':')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (segments.size < 3) return null

    for (index in 0..(segments.size - 3)) {
        if (segments[index].equals("roku", ignoreCase = true) &&
            segments[index + 1].equals("ecp", ignoreCase = true)
        ) {
            return segments[index + 2].asRokuSerialOrNull()
        }
    }

    return null
}

internal fun canonicalRokuIdentity(
    serialNumber: String?,
    ipAddress: String,
    port: Int,
    fallbackUniqueId: String? = null
): RokuDiscoveryIdentity {
    val uniqueId = serialNumber?.trim()?.takeIf { it.isNotEmpty() }
        ?: fallbackUniqueId?.trim()?.takeIf { it.isNotEmpty() }
        ?: "manual:$ipAddress:$port"
    return RokuDiscoveryIdentity(
        id = "roku:${uniqueId.lowercase(Locale.US)}",
        uniqueId = uniqueId
    )
}

private fun String.asRokuSerialOrNull(): String? {
    val serial = trim().trim('"')
    return serial.takeIf { candidate ->
        candidate.isNotEmpty() &&
            candidate.any { it.isLetterOrDigit() } &&
            candidate.none { it.isWhitespace() || it == ':' }
    }
}
