package com.rgremote.app.roku

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.discovery.canonicalRokuIdentity
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class RokuConnectionCoordinator(
    private val rokuAdapter: RokuEcpGateway,
    private val rescan: suspend () -> Int,
    private val lookup: suspend (String) -> RegisteredDevice?,
    private val rekey: suspend (staleDeviceId: String, replacement: RegisteredDevice) -> Unit =
        { _, _ -> },
    private val retryDelaysMillis: List<Long> = DEFAULT_RETRY_DELAYS,
) {
    constructor(
        discoveryService: DiscoveryService,
        registry: DeviceRegistry,
        rokuAdapter: RokuEcpClient,
    ) : this(
        rokuAdapter = rokuAdapter,
        rescan = { discoveryService.scanRoku() },
        lookup = { id -> registry.getDevice(id) },
        rekey = { staleDeviceId, replacement ->
            registry.rekeyDevice(staleDeviceId, replacement)
        },
    )

    suspend fun resolveDevice(device: RegisteredDevice): RegisteredDevice =
        lookup(device.id) ?: device

    suspend fun probeDeviceInfo(device: RegisteredDevice): Result<RokuDeviceInfo> {
        val target = resolveDevice(device)
        val probe = retryEcp(maxAttempts = DEFAULT_PROBE_ATTEMPTS) {
            rokuAdapter.queryDeviceInfo(target)
        }
        if (probe.isSuccess) {
            promoteToSerialIdentity(target, probe.getOrThrow())
            return probe
        }
        if (probe.exceptionOrNull()?.isRetryable() != true) return probe

        val rescanned = rescanAndResolve(target) ?: return probe
        val recovered = retryEcp(maxAttempts = RECOVERY_PROBE_ATTEMPTS) {
            rokuAdapter.queryDeviceInfo(rescanned)
        }
        if (recovered.isSuccess) {
            promoteToSerialIdentity(rescanned, recovered.getOrThrow())
        }
        return recovered
    }

    suspend fun queryActiveApp(device: RegisteredDevice) =
        retryEcp(maxAttempts = DEFAULT_PROBE_ATTEMPTS) {
            rokuAdapter.queryActiveApp(resolveDevice(device))
        }

    suspend fun queryApps(device: RegisteredDevice): Result<List<RokuApp>> {
        val target = resolveDevice(device)
        val query = retryEcp(maxAttempts = DEFAULT_PROBE_ATTEMPTS) {
            rokuAdapter.queryApps(target)
        }
        if (query.isSuccess) return query
        if (query.exceptionOrNull()?.isRetryable() != true) return query

        val rescanned = rescanAndResolve(target) ?: return query
        return retryEcp(maxAttempts = RECOVERY_PROBE_ATTEMPTS) {
            rokuAdapter.queryApps(rescanned)
        }
    }

    suspend fun sendWithRecovery(
        device: RegisteredDevice,
        command: RemoteCommand,
    ): Result<Unit> {
        val target = resolveDevice(device)
        val result = retrySend(target, command, maxAttempts = sendAttemptsFor(command))
        if (result.isSuccess) return result
        if (result.exceptionOrNull()?.isRetryable() != true) return result

        // Connection recovery (rescan + registry refresh) is allowed for every command;
        // only the command POST itself must stay single-shot for non-idempotent ones.
        val rescanned = rescanAndResolve(target) ?: return result
        if (!command.isRetrySafe()) return result
        return retrySend(rescanned, command, maxAttempts = RECOVERY_SEND_ATTEMPTS)
    }

    /**
     * Manual-keyed devices are persisted under their canonical serial identity as soon as
     * an identity probe reveals the serial number, so later DHCP changes recover by serial.
     */
    private suspend fun promoteToSerialIdentity(source: RegisteredDevice, info: RokuDeviceInfo) {
        val serial = info.serialNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val identity = canonicalRokuIdentity(
            serialNumber = serial,
            ipAddress = source.ipAddress,
            port = source.port,
        )
        if (identity.id == source.id) return
        rekey(source.id, source.copy(id = identity.id, uniqueId = identity.uniqueId))
    }

    /** Commands whose delivery converges to the same device state when repeated. */
    private fun RemoteCommand.isRetrySafe(): Boolean = when (this) {
        is RemoteCommand.SetInput -> true
        RemoteCommand.PowerOn, RemoteCommand.PowerOff -> true
        else -> false
    }

    private fun sendAttemptsFor(command: RemoteCommand): Int =
        if (command.isRetrySafe()) DEFAULT_SEND_ATTEMPTS else SINGLE_SEND_ATTEMPT

    private suspend fun rescanAndResolve(device: RegisteredDevice): RegisteredDevice? {
        try {
            rescan()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The passive listener may already have updated the device address.
        }
        return lookup(device.id)
    }

    private suspend fun retrySend(
        device: RegisteredDevice,
        command: RemoteCommand,
        maxAttempts: Int = DEFAULT_SEND_ATTEMPTS,
    ): Result<Unit> {
        var last: Result<Unit> = Result.failure(IOException("Roku command not attempted"))
        repeat(maxAttempts) { attempt ->
            last = rokuAdapter.send(device, command)
            val error = last.exceptionOrNull() ?: return last
            if (error is CancellationException) throw error
            if (!error.isRetryable()) return last
            if (attempt < maxAttempts - 1) {
                delay(retryDelaysMillis.getOrElse(attempt) { 500L })
            }
        }
        return last
    }

    private suspend fun <T> retryEcp(
        maxAttempts: Int,
        block: suspend () -> T,
    ): Result<T> {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(block())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!error.isRetryable()) return Result.failure(error)
                lastError = error
            }
            if (attempt < maxAttempts - 1) {
                delay(retryDelaysMillis.getOrElse(attempt) { 500L })
            }
        }
        return Result.failure(lastError ?: IOException("Roku ECP request failed"))
    }

    private fun Throwable.isRetryable(): Boolean =
        this is IOException && this !is RokuNetworkAccessException &&
            !(this is RokuHttpStatusException && code in 400..499)

    companion object {
        private val DEFAULT_RETRY_DELAYS = listOf(200L, 500L)
        private const val DEFAULT_PROBE_ATTEMPTS = 3
        private const val DEFAULT_SEND_ATTEMPTS = 3
        private const val SINGLE_SEND_ATTEMPT = 1
        private const val RECOVERY_PROBE_ATTEMPTS = 2
        private const val RECOVERY_SEND_ATTEMPTS = 2
    }
}
