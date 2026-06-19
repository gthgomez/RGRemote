package com.rgremote.app.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RokuSsdpListener(
    context: Context,
    private val onDeviceDiscovered: (RegisteredDevice) -> Unit
) {
    private val appContext = context.applicationContext
    private var job: Job? = null
    private var socket: MulticastSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return // Already running

        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifi?.createMulticastLock("RGRemote:RokuSsdpListener")?.apply {
            setReferenceCounted(false)
            acquire()
        }

        job = scope.launch(Dispatchers.IO) {
            try {
                val group = InetAddress.getByName("239.255.255.250")
                val mSocket = MulticastSocket(1900).apply {
                    reuseAddress = true
                }
                socket = mSocket
                
                val groupAddress = InetSocketAddress(group, 1900)
                val netIf = NetworkInterface.getNetworkInterfaces()?.asSequence()?.firstOrNull { ni ->
                    ni.isUp && ni.supportsMulticast() && !ni.isLoopback
                }
                mSocket.joinGroup(groupAddress, netIf)

                val buffer = ByteArray(4096)
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        mSocket.receive(packet)
                    } catch (e: Exception) {
                        break // Socket closed or thread cancelled
                    }

                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val lines = message.lineSequence()
                        .mapNotNull { line ->
                            val idx = line.indexOf(':')
                            if (idx <= 0) null else {
                                line.substring(0, idx).trim().lowercase(Locale.US) to
                                    line.substring(idx + 1).trim()
                            }
                        }
                        .toMap()

                    // Check if the announcement corresponds to a Roku ECP device
                    val isRoku = lines["st"]?.contains("roku:ecp", ignoreCase = true) == true ||
                        lines["usn"]?.contains("roku:ecp", ignoreCase = true) == true ||
                        lines["nt"]?.contains("roku:ecp", ignoreCase = true) == true

                    if (isRoku) {
                        val location = lines["location"]
                        if (location != null) {
                            val uri = runCatching { java.net.URI(location) }.getOrNull()
                            val ip = uri?.host ?: packet.address.hostAddress
                            if (ip != null) {
                                val port = if (uri != null && uri.port > 0) uri.port else 8060
                                val serial = extractRokuSerialFromUsn(lines["usn"])
                                val identity = canonicalRokuIdentity(
                                    serialNumber = serial,
                                    ipAddress = ip,
                                    port = port
                                )
                                val device = RegisteredDevice(
                                    id = identity.id,
                                    type = DeviceType.ROKU_TV,
                                    ipAddress = ip,
                                    port = port,
                                    uniqueId = identity.uniqueId,
                                    friendlyName = "Roku TV $ip",
                                    lastSeenMillis = System.currentTimeMillis(),
                                    hdmiPortMapping = null,
                                    isOnline = true,
                                    consecutiveFailures = 0
                                )
                                withContext(Dispatchers.Main) {
                                    onDeviceDiscovered(device)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cleanup()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        cleanup()
    }

    private fun cleanup() {
        try {
            socket?.let { s ->
                val group = InetAddress.getByName("239.255.255.250")
                val groupAddress = InetSocketAddress(group, 1900)
                val netIf = NetworkInterface.getNetworkInterfaces()?.asSequence()?.firstOrNull { ni ->
                    ni.isUp && ni.supportsMulticast() && !ni.isLoopback
                }
                runCatching { s.leaveGroup(groupAddress, netIf) }
                s.close()
            }
        } catch (e: Exception) {
            // Ignore
        }
        socket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        multicastLock = null
    }
}
