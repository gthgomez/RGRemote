package com.rgremote.app.roku

import com.rgremote.app.domain.ActiveApp
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.DpadDirection
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RokuConnectionCoordinatorTest {
    @Test
    fun sendWithRecovery_retrySafeCommandResendsAfterRescan() = runBlocking {
        var sendAttempts = 0
        var scanCalls = 0
        var device = testDevice(ipAddress = "192.168.1.10")
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> {
                sendAttempts++
                return if (sendAttempts <= 3) {
                    Result.failure(IOException("timeout"))
                } else {
                    Result.success(Unit)
                }
            }

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
                throw UnsupportedOperationException()

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? =
                throw UnsupportedOperationException()

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> =
                throw UnsupportedOperationException()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = {
                scanCalls++
                device = device.copy(ipAddress = "192.168.1.11")
                1
            },
            lookup = { _ -> device },
            retryDelaysMillis = listOf(1L, 1L),
        )

        val result = coordinator.sendWithRecovery(
            device,
            RemoteCommand.SetInput(HdmiPort.HDMI2)
        )

        assertTrue(result.isSuccess)
        assertTrue(scanCalls >= 1)
        assertEquals(4, sendAttempts)
    }

    @Test
    fun sendWithRecovery_nonIdempotentCommandSendsExactlyOnce() = runBlocking {
        var sendAttempts = 0
        var scanCalls = 0
        val device = testDevice()
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> {
                sendAttempts++
                return Result.failure(IOException("timeout"))
            }

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
                throw UnsupportedOperationException()

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? =
                throw UnsupportedOperationException()

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> =
                throw UnsupportedOperationException()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = {
                scanCalls++
                1
            },
            lookup = { _ -> device },
            retryDelaysMillis = listOf(1L, 1L),
        )

        val result = coordinator.sendWithRecovery(
            device,
            RemoteCommand.Dpad(DpadDirection.UP)
        )

        assertTrue(result.isFailure)
        assertEquals(1, sendAttempts)
        assertEquals(1, scanCalls)
    }

    @Test
    fun probeDeviceInfo_promotesManualDeviceToSerialIdentity() = runBlocking {
        var rekeyedFrom: String? = null
        var rekeyedTo: RegisteredDevice? = null
        val device = testDevice(id = "roku:manual:192.168.1.50:8060")
            .copy(uniqueId = "manual:192.168.1.50:8060")
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
                Result.failure(UnsupportedOperationException())

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
                RokuDeviceInfo(
                    friendlyName = "Living Room",
                    serialNumber = "ABC123",
                    powerMode = "DisplayOff"
                )

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? = null

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> = emptyList()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = { 0 },
            lookup = { _ -> device },
            rekey = { staleDeviceId, replacement ->
                rekeyedFrom = staleDeviceId
                rekeyedTo = replacement
            },
            retryDelaysMillis = listOf(1L),
        )

        val result = coordinator.probeDeviceInfo(device)

        assertTrue(result.isSuccess)
        assertEquals("roku:manual:192.168.1.50:8060", rekeyedFrom)
        assertEquals("roku:abc123", rekeyedTo?.id)
        assertEquals("ABC123", rekeyedTo?.uniqueId)
    }

    @Test
    fun probeDeviceInfo_serialKeyedDeviceIsNotRekeyed() = runBlocking {
        var rekeyInvocations = 0
        val device = testDevice(id = "roku:abc123").copy(uniqueId = "ABC123")
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
                Result.failure(UnsupportedOperationException())

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
                RokuDeviceInfo(friendlyName = null, serialNumber = "ABC123", powerMode = null)

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? = null

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> = emptyList()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = { 0 },
            lookup = { _ -> device },
            rekey = { _, _ -> rekeyInvocations++ },
            retryDelaysMillis = listOf(1L),
        )

        val result = coordinator.probeDeviceInfo(device)

        assertTrue(result.isSuccess)
        assertEquals(0, rekeyInvocations)
    }

    @Test
    fun probeDeviceInfo_skipsRescanWhenNetworkAccessDenied() = runBlocking {
        var scanCalls = 0
        val device = testDevice()
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
                Result.failure(UnsupportedOperationException())

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo =
                throw RokuNetworkAccessException("query/device-info", 403)

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? = null

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> = emptyList()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = {
                scanCalls++
                0
            },
            lookup = { _ -> device },
            retryDelaysMillis = listOf(1L),
        )

        val result = coordinator.probeDeviceInfo(device)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RokuNetworkAccessException)
        assertEquals(0, scanCalls)
    }

    @Test
    fun probeDeviceInfo_retriesBeforeRescan() = runBlocking {
        var probeAttempts = 0
        var scanCalls = 0
        val device = testDevice()
        val gateway = object : RokuEcpGateway {
            override suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit> =
                Result.failure(UnsupportedOperationException())

            override suspend fun queryDeviceInfo(device: RegisteredDevice): RokuDeviceInfo {
                probeAttempts++
                if (probeAttempts < 2) throw IOException("timeout")
                return RokuDeviceInfo(
                    friendlyName = "Living Room",
                    serialNumber = "ABC",
                    powerMode = "DisplayOff",
                )
            }

            override suspend fun queryActiveApp(device: RegisteredDevice): ActiveApp? = null

            override suspend fun queryApps(device: RegisteredDevice): List<RokuApp> = emptyList()
        }
        val coordinator = RokuConnectionCoordinator(
            rokuAdapter = gateway,
            rescan = {
                scanCalls++
                0
            },
            lookup = { _ -> device },
            retryDelaysMillis = listOf(1L),
        )

        val result = coordinator.probeDeviceInfo(device)

        assertTrue(result.isSuccess)
        assertEquals("DisplayOff", result.getOrNull()?.powerMode)
        assertEquals(2, probeAttempts)
        assertEquals(0, scanCalls)
    }

    private fun testDevice(id: String = "roku:test", ipAddress: String = "192.168.1.50"): RegisteredDevice =
        RegisteredDevice(
            id = id,
            type = DeviceType.ROKU_TV,
            ipAddress = ipAddress,
            port = 8060,
            uniqueId = "test",
            friendlyName = "Test Roku",
            lastSeenMillis = 0L,
            hdmiPortMapping = null,
            isOnline = true,
            consecutiveFailures = 0
        )
}
