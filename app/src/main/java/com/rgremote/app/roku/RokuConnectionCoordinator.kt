package com.rgremote.app.roku

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.discovery.DiscoveryService
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo
import java.io.IOException
import kotlinx.coroutines.delay

class RokuConnectionCoordinator(
    private val rokuAdapter: RokuEcpGateway,
    private val rescan: suspend () -> Int,
    private val lookup: suspend (String) -> RegisteredDevice?,
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
    )

    suspend fun resolveDevice(device: RegisteredDevice): RegisteredDevice =
        lookup(device.id) ?: device

    suspend fun probeDeviceInfo(device: RegisteredDevice): Result<RokuDeviceInfo> {
        val target = resolveDevice(device)
        val probe = retryEcp(maxAttempts = DEFAULT_PROBE_ATTEMPTS) {
            rokuAdapter.queryDeviceInfo(target)
        }
        if (probe.isSuccess) return probe
        if (!probe.exceptionOrNull().isRecoverableByRescan()) return probe

        val rescanned = rescanAndResolve(target) ?: return probe
        return retryEcp(maxAttempts = RECOVERY_PROBE_ATTEMPTS) {
            rokuAdapter.queryDeviceInfo(rescanned)
        }
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
        if (!query.exceptionOrNull().isRecoverableByRescan()) return query

        val rescanned = rescanAndResolve(target) ?: return query
        return retryEcp(maxAttempts = RECOVERY_PROBE_ATTEMPTS) {
            rokuAdapter.queryApps(rescanned)
        }
    }

    suspend fun sendWithRecovery(
        device: RegisteredDevice,
        command: RemoteCommand,
    ): Result<Unit> {
        var target = resolveDevice(device)
        var result = retrySend(target, command)
        if (result.isSuccess) return result
        if (!result.exceptionOrNull().isRecoverableByRescan()) return result

        val rescanned = rescanAndResolve(target) ?: return result
        target = rescanned
        return retrySend(target, command, maxAttempts = RECOVERY_SEND_ATTEMPTS)
    }

    private suspend fun rescanAndResolve(device: RegisteredDevice): RegisteredDevice? {
        runCatching { rescan() }
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
            if (last.isSuccess) return last
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
            runCatching { block() }
                .onSuccess { return Result.success(it) }
                .onFailure { error ->
                    lastError = error
                    if (attempt < maxAttempts - 1) {
                        delay(retryDelaysMillis.getOrElse(attempt) { 500L })
                    }
                }
        }
        return Result.failure(lastError ?: IOException("Roku ECP request failed"))
    }

    private fun Throwable?.isRecoverableByRescan(): Boolean =
        this != null && this !is RokuNetworkAccessException

    companion object {
        private val DEFAULT_RETRY_DELAYS = listOf(200L, 500L)
        private const val DEFAULT_PROBE_ATTEMPTS = 3
        private const val DEFAULT_SEND_ATTEMPTS = 3
        private const val RECOVERY_PROBE_ATTEMPTS = 2
        private const val RECOVERY_SEND_ATTEMPTS = 2
    }
}
