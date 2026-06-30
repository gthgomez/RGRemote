package com.rgremote.app.data.registry

import com.rgremote.app.data.db.DeviceDao
import com.rgremote.app.data.db.DeviceEntity
import com.rgremote.app.data.db.PairingCredentialEntity
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.domain.canonicalDevicesPerType
import com.rgremote.app.domain.sameEndpointAs
import com.rgremote.app.domain.DEVICE_COMPARATOR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeviceRegistry(private val dao: DeviceDao) {
    val devices: Flow<List<RegisteredDevice>> =
        dao.observeDevices().map { rows -> rows.map { it.toDomain() } }

    val pairedDeviceIds: Flow<Set<String>> =
        dao.observePairedDeviceIds().map { ids -> ids.toSet() }

    suspend fun getDevice(id: String): RegisteredDevice? =
        dao.getDevice(id)?.toDomain()

    suspend fun upsertDiscoveredDevice(device: RegisteredDevice) {
        dao.deleteDevicesAtEndpointExcept(
            keepId = device.id,
            type = device.type.name,
            ipAddress = device.ipAddress,
            port = device.port
        )
        val existing = dao.getDevice(device.id)?.toDomain()
        dao.upsertDevice(
            device.copy(
                hdmiPortMapping = device.hdmiPortMapping ?: existing?.hdmiPortMapping
            ).toEntity()
        )
        // dedupeStoredDevices() is intentionally NOT called here.
        // It is an O(n) full-table scan; calling it per upsert causes excessive DB churn
        // during SSDP floods. The ViewModel runs a single debounced dedupe after each
        // scan completes (see RGRemoteViewModel.scanRoku / startForegroundPolling).
    }

    suspend fun removeDevice(deviceId: String) {
        dao.deletePairingCredential(deviceId)
        dao.deleteDevice(deviceId)
        dedupeStoredDevices()
    }

    /** Removes duplicate rows (same physical TV, different ids after scan/reinstall). */
    suspend fun dedupeStoredDevices() {
        val all = dao.listDevices().map { it.toDomain() }
        if (all.isEmpty()) return

        // Group by physical device identity: real serial number if available, otherwise fallback to IP
        val groups = all.groupBy { device ->
            if (device.uniqueId.startsWith("manual:")) {
                "ip:${device.ipAddress}"
            } else {
                "serial:${device.uniqueId.lowercase()}"
            }
        }

        groups.forEach { (_, groupDevices) ->
            if (groupDevices.isEmpty()) return@forEach

            // Pick the best representative to keep (prefer real serial over manual entry)
            val canonical = groupDevices.maxWithOrNull(DEVICE_COMPARATOR) ?: groupDevices.first()

            val duplicates = groupDevices.filter { it.id != canonical.id }
            if (duplicates.isNotEmpty()) {
                val merged = duplicates.fold(canonical) { acc, dup ->
                    acc.copy(
                        hdmiPortMapping = acc.hdmiPortMapping ?: dup.hdmiPortMapping,
                        friendlyName = if (acc.friendlyName.length >= dup.friendlyName.length) acc.friendlyName else dup.friendlyName,
                        lastSeenMillis = maxOf(acc.lastSeenMillis, dup.lastSeenMillis),
                        wifiMac = acc.wifiMac ?: dup.wifiMac,
                        ethernetMac = acc.ethernetMac ?: dup.ethernetMac
                    )
                }
                dao.upsertDevice(merged.toEntity())

                duplicates.forEach { stale ->
                    migratePairingCredentialIfNeeded(stale, listOf(canonical))
                    dao.deletePairingCredential(stale.id)
                    dao.deleteDevice(stale.id)
                }
            }
        }
    }

    private suspend fun migratePairingCredentialIfNeeded(
        stale: RegisteredDevice,
        keep: List<RegisteredDevice>,
    ) {
        val credential = dao.getPairingCredential(stale.id) ?: return
        val target = keep.firstOrNull { it.type == stale.type } ?: return
        if (dao.getPairingCredential(target.id) != null) return
        dao.upsertPairingCredential(credential.copy(deviceId = target.id))
    }

    suspend fun setHdmiMapping(deviceId: String, port: HdmiPort?) {
        dao.setHdmiMapping(deviceId, port?.name)
    }

    suspend fun markCommandSuccess(device: RegisteredDevice) {
        dao.updateHealth(device.id, isOnline = true, failures = 0)
    }

    suspend fun markCommandFailure(device: RegisteredDevice) {
        dao.incrementConsecutiveFailures(device.id)
    }

    suspend fun getCredential(deviceId: String): PairingCredentialEntity? =
        dao.getPairingCredential(deviceId)

    suspend fun saveCredential(credential: PairingCredentialEntity) {
        dao.upsertPairingCredential(credential)
    }

    suspend fun deleteCredential(deviceId: String) {
        dao.deletePairingCredential(deviceId)
    }
}

private fun DeviceEntity.toDomain(): RegisteredDevice =
    RegisteredDevice(
        id = id,
        type = DeviceType.valueOf(type),
        ipAddress = ipAddress,
        port = port,
        uniqueId = uniqueId,
        friendlyName = friendlyName,
        lastSeenMillis = lastSeenMillis,
        hdmiPortMapping = HdmiPort.fromName(hdmiPortMapping),
        isOnline = isOnline,
        consecutiveFailures = consecutiveFailures,
        wifiMac = wifiMac,
        ethernetMac = ethernetMac
    )

private fun RegisteredDevice.toEntity(): DeviceEntity =
    DeviceEntity(
        id = id,
        type = type.name,
        ipAddress = ipAddress,
        port = port,
        uniqueId = uniqueId,
        serviceName = null,
        friendlyName = friendlyName,
        lastSeenMillis = lastSeenMillis,
        hdmiPortMapping = hdmiPortMapping?.name,
        isOnline = isOnline,
        consecutiveFailures = consecutiveFailures,
        wifiMac = wifiMac,
        ethernetMac = ethernetMac
    )
