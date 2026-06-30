package com.rgremote.app.discovery

import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.roku.RokuEcpClient

class DiscoveryService(
    private val registry: DeviceRegistry,
    private val rokuSsdpDiscovery: RokuSsdpDiscovery,
    private val googleTvNsdDiscovery: GoogleTvNsdDiscovery,
    private val rokuClient: RokuEcpClient
) {
    suspend fun scanRoku(): Int {
        val found = rokuSsdpDiscovery.scan()
        found.forEach { discovered ->
            val enriched = if (discovered.type == DeviceType.ROKU_TV) {
                runCatching {
                    val info = rokuClient.queryDeviceInfo(discovered)
                    val identity = canonicalRokuIdentity(
                        serialNumber = info.serialNumber,
                        ipAddress = discovered.ipAddress,
                        port = discovered.port,
                        fallbackUniqueId = discovered.uniqueId
                    )
                    discovered.copy(
                        id = identity.id,
                        friendlyName = info.friendlyName ?: discovered.friendlyName,
                        uniqueId = identity.uniqueId,
                        wifiMac = info.wifiMac,
                        ethernetMac = info.ethernetMac
                    )
                }.getOrDefault(discovered)
            } else {
                discovered
            }
            registry.upsertDiscoveredDevice(enriched)
        }
        return found.size
    }

    fun startGoogleTvDiscovery(onError: (String) -> Unit) {
        googleTvNsdDiscovery.start(
            onDevice = { device -> saveAsync(device) },
            onError = onError
        )
    }

    fun stopGoogleTvDiscovery() {
        googleTvNsdDiscovery.stop()
    }

    fun destroy() {
        googleTvNsdDiscovery.destroy()
    }

    var saveAsync: (RegisteredDevice) -> Unit = {}
}
