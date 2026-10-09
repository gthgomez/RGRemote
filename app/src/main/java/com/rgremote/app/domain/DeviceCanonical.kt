package com.rgremote.app.domain

/**
 * Picks one saved row per [DeviceType] for UI and control.
 * Duplicate rows usually come from discovery assigning a new id after serial enrichment.
 */
val DEVICE_COMPARATOR: Comparator<RegisteredDevice> =
    compareBy<RegisteredDevice> { !it.uniqueId.startsWith("manual:") }
        .thenBy { it.isOnline }
        .thenBy { it.consecutiveFailures == 0 }
        .thenBy { it.lastSeenMillis }
        .thenBy { it.hdmiPortMapping != null }

fun List<RegisteredDevice>.canonicalDevicesPerType(): List<RegisteredDevice> {
    val groups = groupBy { device ->
        val identity = if (device.uniqueId.startsWith("manual:")) {
            "ip:${device.ipAddress}:${device.port}"
        } else {
            "serial:${device.uniqueId.lowercase()}"
        }
        "${device.type}:$identity"
    }
    return groups.map { (_, groupDevices) ->
        groupDevices.maxWithOrNull(DEVICE_COMPARATOR) ?: groupDevices.first()
    }
}

fun List<RegisteredDevice>.duplicateDeviceCount(): Int =
    (size - canonicalDevicesPerType().size).coerceAtLeast(0)

fun RegisteredDevice.sameEndpointAs(other: RegisteredDevice): Boolean =
    type == other.type &&
        ipAddress.equals(other.ipAddress, ignoreCase = true) &&
        port == other.port
