package com.rgremote.app.domain

/**
 * Picks one saved row per [DeviceType] for UI and control.
 * Duplicate rows usually come from discovery assigning a new id after serial enrichment.
 */
fun List<RegisteredDevice>.canonicalDevicesPerType(): List<RegisteredDevice> =
    DeviceType.entries.mapNotNull { type ->
        filter { it.type == type }.maxWithOrNull(devicePreferenceComparator())
    }

fun List<RegisteredDevice>.duplicateDeviceCount(): Int =
    (size - canonicalDevicesPerType().size).coerceAtLeast(0)

private fun devicePreferenceComparator(): Comparator<RegisteredDevice> =
    compareBy<RegisteredDevice> { it.hdmiPortMapping != null }
        .thenBy { it.isOnline }
        .thenBy { it.consecutiveFailures == 0 }
        .thenBy { it.lastSeenMillis }

fun RegisteredDevice.sameEndpointAs(other: RegisteredDevice): Boolean =
    type == other.type &&
        ipAddress.equals(other.ipAddress, ignoreCase = true) &&
        port == other.port
