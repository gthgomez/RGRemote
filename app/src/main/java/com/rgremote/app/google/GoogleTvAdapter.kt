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
import kotlinx.coroutines.withTimeoutOrNull

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

    /** Set by [shutdown]; blocks new sends and fresh reconnects so no socket outlives shutdown. */
    @Volatile private var isShutdown = false
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
        runCatching {
            check(!isShutdown) { "Google TV adapter is shut down" }
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
        if (isShutdown) {
            throw IllegalStateException("Google TV adapter is shut down")
        }
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
        } catch (error: Throwable) {
            // Any throwable must release the connected socket (e.g., a RuntimeException
            // during negotiation), not just IOException, or the socket leaks.
            runCatching { socket.close() }
            throw error
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

    /**
     * Shuts the adapter down without ever blocking the calling thread indefinitely
     * (invoked from ViewModel.onCleared on the main thread).
     *
     * Ordering: mark shut down (new sends and reconnects fail fast) → cancel [adapterScope]
     * → snapshot [cachedSession] → acquire [sessionMutex] with a short bounded timeout.
     * If the lock is won, the current session is closed under the mutex; if a concurrent
     * send still holds the mutex mid-network-I/O, only the snapshotted session is closed
     * best-effort without the lock, keeping shutdown bounded. Safe from any thread.
     */
    override fun shutdown() {
        isShutdown = true
        adapterScope.cancel()
        val snapshot = cachedSession
        runCatching {
            runBlocking {
                val acquired = withTimeoutOrNull(SHUTDOWN_LOCK_TIMEOUT_MILLIS) {
                    sessionMutex.lock()
                    true
                } == true
                try {
                    if (acquired) {
                        // Authoritative state under the mutex; may be newer than snapshot.
                        val session = cachedSession
                        if (session != null) {
                            cachedSession = null
                            session.closeQuietly()
                        }
                    } else {
                        // Lock contended: close only the snapshotted session so we never
                        // clobber one the in-flight sender created after our snapshot.
                        snapshot?.closeQuietly()
                    }
                } finally {
                    if (acquired) {
                        sessionMutex.unlock()
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
         * Bound on how long [shutdown] waits for [sessionMutex]. Short so main-thread
         * callers never ANR; the snapshot best-effort close covers the contended case.
         */
        private const val SHUTDOWN_LOCK_TIMEOUT_MILLIS = 250L
        /**
         * Sessions idle longer than this are considered stale: the next send closes the
         * old socket and opens a fresh TLS connection rather than risk a half-dead channel.
         * 45 s is comfortably inside typical OS TCP keep-alive probe windows.
         */
        private const val SESSION_IDLE_TIMEOUT_MILLIS = 45_000L
        private const val TLS_DRAIN_TIMEOUT_MILLIS = 50
    }
}
