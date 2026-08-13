package com.rgremote.app.google

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteAdapter
import com.rgremote.app.domain.RemoteCommand
import java.io.IOException
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GoogleTvAdapter(
    private val registry: DeviceRegistry,
    private val keyStore: GoogleTvKeyStore,
) : RemoteAdapter {

    /**
     * Serializes concurrent sends so rapid button presses queue rather than opening
     * parallel TLS handshakes. [cachedSession] is only accessed while this lock is held.
     */
    private val sessionMutex = Mutex()
    @Volatile private var cachedSession: CachedSession? = null
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
        runCatching {
            val credential = registry.getCredential(device.id)
                ?: throw IOException("Google TV is not paired yet")
            val serverPin = credential.serverCertificateSha256
                ?: throw IOException("Google TV credential predates certificate pinning; re-pair the device")
            withContext(Dispatchers.IO) {
                sessionMutex.withLock {
                    sendWithSessionCache(device, credential.keyAlias, serverPin, command)
                }
            }
        }

    /**
     * Sends [command] through the persistent TLS session, creating or replacing the session
     * as needed. Must always be called while holding [sessionMutex].
     *
     * Recovery path: if the cached socket reports closed or throws [IOException] (server
     * half-closed, network change, TV went to sleep), the socket is discarded and a fresh
     * handshake runs inline before the command is retried — the caller sees no failure.
     */
    private fun sendWithSessionCache(
        device: RegisteredDevice,
        keyAlias: String,
        serverPin: String,
        command: RemoteCommand,
    ) {
        val existing = cachedSession
        if (existing != null) {
            val stale = existing.deviceId != device.id || existing.socket.isClosed || existing.isIdleExpired()
            if (!stale) {
                try {
                    existing.session.send(command)
                    existing.touch()
                    return
                } catch (_: IOException) {
                    // Session went dead mid-use (server closed, network hiccup, TV slept).
                    // Close quietly and fall through to reconnect below.
                }
            }
            existing.closeQuietly()
            cachedSession = null
        }

        // No usable cached session — open a fresh persistent TLS connection.
        val sslContext = keyStore.pairedSslContext(keyAlias, serverPin)
        val socket = sslContext.socketFactory.createSocket() as SSLSocket
        try {
            socket.soTimeout = READ_TIMEOUT_MILLIS
            socket.connect(
                java.net.InetSocketAddress(device.ipAddress, device.port),
                CONNECT_TIMEOUT_MILLIS
            )
            socket.startHandshake()
            val transport = SslGoogleTvRemoteTransport(
                socket = socket,
                readTimeoutMillis = READ_TIMEOUT_MILLIS,
                drainTimeoutMillis = TLS_DRAIN_TIMEOUT_MILLIS,
            )
            val session = GoogleTvRemoteV2Session(transport)
            val fresh = CachedSession(device.id, socket, session)
            session.send(command)   // negotiate + send; only cache on success
            fresh.touch()
            cachedSession = fresh
        } catch (e: IOException) {
            runCatching { socket.close() }
            throw e
        }
    }

    /**
     * Forcibly closes and discards the cached TLS session.
     *
     * Pass [deviceId] to only invalidate the session for that device (e.g., after re-pair);
     * pass null to unconditionally invalidate any cached session.
     */
    override fun invalidateSession(deviceId: String?) {
        // Capture the session that was current at invocation time to prevent
        // closing a session created after this call (e.g., by a racing send()).
        val target = cachedSession ?: return
        if (deviceId != null && target.deviceId != deviceId) return
        adapterScope.launch {
            sessionMutex.withLock {
                // Re-check under the mutex: only close if the reference
                // hasn't been replaced between our snapshot and now.
                if (cachedSession === target) {
                    cachedSession = null
                    target.closeQuietly()
                }
            }
        }
    }

    override fun shutdown() {
        adapterScope.cancel()
        runCatching {
            kotlinx.coroutines.runBlocking {
                sessionMutex.withLock {
                    val session = cachedSession
                    if (session != null) {
                        cachedSession = null
                        session.closeQuietly()
                    }
                }
            }
        }
    }

    private class CachedSession(
        val deviceId: String,
        val socket: SSLSocket,
        val session: GoogleTvRemoteV2Session,
    ) {
        private var lastUsedMillis: Long = System.currentTimeMillis()

        /** Updates the last-used timestamp so the idle timer resets on each command. */
        fun touch() {
            lastUsedMillis = System.currentTimeMillis()
        }

        /** Returns true if the session has been idle longer than [SESSION_IDLE_TIMEOUT_MILLIS]. */
        fun isIdleExpired(): Boolean =
            System.currentTimeMillis() - lastUsedMillis > SESSION_IDLE_TIMEOUT_MILLIS

        /** Closes the underlying socket without propagating any exception. */
        fun closeQuietly() {
            runCatching { socket.close() }
        }
    }

    companion object {
        private const val READ_TIMEOUT_MILLIS = 1_500
        private const val CONNECT_TIMEOUT_MILLIS = 3_000
        /**
         * Sessions idle longer than this are considered stale: the next send closes the
         * old socket and opens a fresh TLS connection rather than risk a half-dead channel.
         * 45 s is comfortably inside typical OS TCP keep-alive probe windows.
         */
        private const val SESSION_IDLE_TIMEOUT_MILLIS = 45_000L
        private const val TLS_DRAIN_TIMEOUT_MILLIS = 50
    }
}
