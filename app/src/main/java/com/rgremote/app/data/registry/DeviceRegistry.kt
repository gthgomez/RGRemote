package com.rgremote.app.data.registry

import com.rgremote.app.data.db.DeviceDao
import com.rgremote.app.data.db.DeviceEntity
import com.rgremote.app.data.db.PairingCredentialEntity
import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RegisteredDevice
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
        dao.upsertAtEndpointReplacingDuplicates(device.toEntity())
        // dedupeStoredDevices() is intentionally NOT called here.
        // It is an O(n) full-table scan; calling it per upsert causes excessive DB churn
        // during SSDP floods. The ViewModel runs a single debounced dedupe after each
        // scan completes (see RGRemoteViewModel.scanRoku / startForegroundPolling).
    }

    /**
     * Atomically moves [staleDeviceId]'s row onto [replacement]: upserts the replacement
     * (merging into an existing row with the same id), migrates the pairing credential,
     * and deletes the stale row in a single transaction.
     */
    suspend fun rekeyDevice(staleDeviceId: String, replacement: RegisteredDevice): RegisteredDevice {
        dao.rekeyDeviceIdentity(staleDeviceId, replacement.toEntity())
        return dao.getDevice(replacement.id)?.toDomain() ?: replacement
    }

    suspend fun removeDevice(deviceId: String) {
        dao.removeDeviceAndDedupe(deviceId)
    }

    /** Removes duplicate rows (same physical TV, different ids after scan/reinstall). */
    suspend fun dedupeStoredDevices() {
        dao.deduplicateStoredDevices()
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
