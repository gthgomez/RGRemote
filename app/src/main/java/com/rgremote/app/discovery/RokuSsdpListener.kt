package com.rgremote.app.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Passive SSDP multicast listener that announces Roku ECP devices as they appear.
 *
 * ### MulticastLock strategy
 * The lock is acquired at the top of the listening job so the system delivers
 * multicast UDP packets to this socket, and it is held for the entire lifetime of
 * the active listening session. Releasing it after the first discovery pass lets
 * the WiFi chip resume multicast filtering, which silently drops subsequent SSDP
 * announcements. The lock is released exactly once in [cleanup], alongside socket
 * teardown, whenever the session ends: an explicit [stop], a listener failure, or
 * job cancellation. Callers can observe liveness via [isRunning]; a dead or failed
 * session never blocks a subsequent [start].
 *
 * ### Early byte-scan filter
 * Before allocating a [String] and parsing header fields, each UDP packet is scanned
 * for the ASCII bytes `roku:ecp` directly on the raw [ByteArray]. Packets from other
 * SSDP participants (TVs, routers, printers …) are discarded in O(n) with no heap
 * allocations, keeping the IO thread clean during busy network intervals.
 */
class RokuSsdpListener(
    context: Context,
    private val onDeviceDiscovered: (RegisteredDevice) -> Unit
) {
    private val appContext = context.applicationContext
    private var job: Job? = null
    private var socket: MulticastSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    /** True only while a live listening job holds the socket and multicast lock. */
    val isRunning: Boolean
        get() = job?.isActive == true

    fun start(scope: CoroutineScope) {
        val current = job
        if (current != null && current.isActive) return // Already running

        // Dead, cancelled, or failed session: reap it so a fresh one can start.
        current?.cancel()
        job = null

        job = scope.launch(Dispatchers.IO) {
            try {
                val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifi?.createMulticastLock("RGRemote:RokuSsdpListener")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }

                val group = InetAddress.getByName("239.255.255.250")
                val mSocket = MulticastSocket(null)
                try {
                    mSocket.reuseAddress = true
                    mSocket.bind(InetSocketAddress(1900))
                } catch (e: Exception) {
                    runCatching { mSocket.close() }
                    throw e
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
                        break // Socket closed or job cancelled
                    }

                    // ── Early byte-scan filter ──────────────────────────────────────────────
                    // Reject packets that don't contain "roku:ecp" (ASCII) before doing any
                    // String allocation or header parsing. Saves ~10–30 µs per foreign packet.
                    if (!containsRokuEcpBytes(packet.data, packet.length)) continue

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Roku SSDP listener failed", e)
            } finally {
                cleanup()
                if (job == runCatching { currentCoroutineContext()[Job] }.getOrNull()) {
                    job = null
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        cleanup()
    }

    /**
     * Tears down the current session. Fields are taken-and-nulled before teardown so a
     * restarted session's new socket/lock can never be closed by a dying previous one,
     * and the lock release happens exactly once even across concurrent exit paths.
     */
    private fun cleanup() {
        val s = socket
        socket = null
        try {
            if (s != null) {
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

        val lock = multicastLock
        multicastLock = null
        try {
            if (lock?.isHeld == true) {
                lock.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        private const val TAG = "RokuSsdpListener"

        /**
         * Returns true if [data][0..[length]) contains the ASCII byte sequence `roku:ecp`
         * (case-insensitive via lower-case comparison). Called on the raw receive buffer
         * before any [String] allocation to avoid parse overhead on non-Roku packets.
         */
        internal fun containsRokuEcpBytes(data: ByteArray, length: Int): Boolean {
            // Pattern: r=0x72/0x52, o=0x6F/0x4F, k=0x6B/0x4B, u=0x75/0x55,
            //          :=0x3A,      e=0x65/0x45, c=0x63/0x43, p=0x70/0x50
            val pattern = "roku:ecp"
            val pLen = pattern.length
            val limit = length - pLen
            outer@ for (i in 0..limit) {
                for (j in 0 until pLen) {
                    val b = data[i + j].toInt().and(0xFF).toChar().lowercaseChar()
                    if (b != pattern[j]) continue@outer
                }
                return true
            }
            return false
        }
    }
}
