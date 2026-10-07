package com.rgremote.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY type, friendlyName")
    fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDevice(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: DeviceEntity)

    @Query("UPDATE devices SET hdmiPortMapping = :hdmiPort WHERE id = :deviceId")
    suspend fun setHdmiMapping(deviceId: String, hdmiPort: String?)

    @Query(
        "UPDATE devices SET isOnline = :isOnline, consecutiveFailures = :failures " +
            "WHERE id = :deviceId"
    )
    suspend fun updateHealth(deviceId: String, isOnline: Boolean, failures: Int)

    @Query(
        "UPDATE devices " +
            "SET isOnline = CASE WHEN consecutiveFailures + 1 < 3 THEN 1 ELSE 0 END, " +
            "    consecutiveFailures = consecutiveFailures + 1 " +
            "WHERE id = :deviceId"
    )
    suspend fun incrementConsecutiveFailures(deviceId: String)

    @Query("SELECT * FROM pairing_credentials WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPairingCredential(deviceId: String): PairingCredentialEntity?

    @Query("SELECT deviceId FROM pairing_credentials")
    fun observePairedDeviceIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPairingCredential(credential: PairingCredentialEntity)

    @Query("DELETE FROM pairing_credentials WHERE deviceId = :deviceId")
    suspend fun deletePairingCredential(deviceId: String)

    @Query("SELECT * FROM devices")
    suspend fun listDevices(): List<DeviceEntity>

    @Query(
        "DELETE FROM devices WHERE id != :keepId AND type = :type " +
            "AND ipAddress = :ipAddress AND port = :port"
    )
    suspend fun deleteDevicesAtEndpointExcept(
        keepId: String,
        type: String,
        ipAddress: String,
        port: Int
    )

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Transaction
    suspend fun upsertAtEndpointReplacingDuplicates(device: DeviceEntity) {
        deleteDevicesAtEndpointExcept(
            keepId = device.id,
            type = device.type,
            ipAddress = device.ipAddress,
            port = device.port
        )
        val previousHdmiPort = device.hdmiPortMapping ?: getDevice(device.id)?.hdmiPortMapping
        upsertDevice(device.copy(hdmiPortMapping = previousHdmiPort))
    }

    @Transaction
    suspend fun removeDeviceAndDedupe(deviceId: String) {
        deletePairingCredential(deviceId)
        deleteDevice(deviceId)
        deduplicateStoredDevices()
    }

    @Transaction
    suspend fun rekeyDeviceIdentity(staleDeviceId: String, replacement: DeviceEntity) {
        val existing = getDevice(replacement.id)
        val merged = if (existing == null) {
            replacement
        } else {
            existing.copy(
                ipAddress = replacement.ipAddress,
                port = replacement.port,
                hdmiPortMapping = existing.hdmiPortMapping ?: replacement.hdmiPortMapping,
                friendlyName = longest(existing.friendlyName, replacement.friendlyName),
                lastSeenMillis = maxOf(existing.lastSeenMillis, replacement.lastSeenMillis),
                wifiMac = existing.wifiMac ?: replacement.wifiMac,
                ethernetMac = existing.ethernetMac ?: replacement.ethernetMac
            )
        }
        upsertDevice(merged)
        migratePairingCredential(staleDeviceId, merged.id)
        deletePairingCredential(staleDeviceId)
        if (staleDeviceId != merged.id) {
            deleteDevice(staleDeviceId)
        }
    }

    /** Removes duplicate rows (same physical TV, different ids after scan/reinstall); returns how many rows were removed. */
    @Transaction
    suspend fun deduplicateStoredDevices(): Int {
        val rows = listDevices()
        if (rows.isEmpty()) return 0

        var removed = 0
        // Group by physical device identity: real serial number if available, otherwise fallback to IP
        val groups = rows.groupBy { row ->
            if (row.uniqueId.startsWith("manual:")) {
                "ip:${row.ipAddress}"
            } else {
                "serial:${row.uniqueId.lowercase()}"
            }
        }

        groups.forEach { (_, groupRows) ->
            if (groupRows.isEmpty()) return@forEach

            // Pick the best representative to keep (prefer real serial over manual entry)
            val canonical = groupRows.maxWithOrNull(DEVICE_ROW_COMPARATOR) ?: return@forEach

            val duplicates = groupRows.filterNot { it.id == canonical.id }
            if (duplicates.isEmpty()) return@forEach

            val merged = duplicates.fold(canonical) { acc, duplicate ->
                acc.copy(
                    hdmiPortMapping = acc.hdmiPortMapping ?: duplicate.hdmiPortMapping,
                    friendlyName = longest(acc.friendlyName, duplicate.friendlyName),
                    lastSeenMillis = maxOf(acc.lastSeenMillis, duplicate.lastSeenMillis),
                    wifiMac = acc.wifiMac ?: duplicate.wifiMac,
                    ethernetMac = acc.ethernetMac ?: duplicate.ethernetMac
                )
            }
            upsertDevice(merged)

            duplicates.forEach { stale ->
                migratePairingCredential(stale.id, merged.id)
                deletePairingCredential(stale.id)
                deleteDevice(stale.id)
            }
            removed += duplicates.size
        }
        return removed
    }

    suspend fun migratePairingCredential(fromDeviceId: String, toDeviceId: String) {
        if (fromDeviceId == toDeviceId) return
        val credential = getPairingCredential(fromDeviceId) ?: return
        if (getPairingCredential(toDeviceId) != null) return
        upsertPairingCredential(credential.copy(deviceId = toDeviceId))
    }

    private fun longest(left: String, right: String): String =
        if (left.length >= right.length) left else right
}

private val DEVICE_ROW_COMPARATOR: Comparator<DeviceEntity> =
    compareBy<DeviceEntity> { it.uniqueId.startsWith("manual:") }
        .thenBy { it.hdmiPortMapping == null }
        .thenBy { !it.isOnline }
        .thenBy { it.consecutiveFailures != 0 }
        .thenByDescending { it.lastSeenMillis }
