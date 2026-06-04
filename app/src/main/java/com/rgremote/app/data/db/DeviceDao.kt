package com.rgremote.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
