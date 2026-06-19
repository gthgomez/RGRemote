package com.rgremote.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val type: String,
    val ipAddress: String,
    val port: Int,
    val uniqueId: String,
    val serviceName: String?,
    val friendlyName: String,
    val lastSeenMillis: Long,
    val hdmiPortMapping: String?,
    val isOnline: Boolean,
    val consecutiveFailures: Int,
    val wifiMac: String? = null,
    val ethernetMac: String? = null
)

@Entity(tableName = "pairing_credentials")
data class PairingCredentialEntity(
    @PrimaryKey val deviceId: String,
    val keyAlias: String,
    val certificatePem: String,
    val serverCertificateSha256: String? = null,
    val protocolVersion: String,
    val pairedAtMillis: Long
)
