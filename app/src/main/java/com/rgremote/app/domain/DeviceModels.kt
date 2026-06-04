package com.rgremote.app.domain

enum class DeviceType {
    ROKU_TV,
    GOOGLE_TV
}

enum class DpadDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

enum class VolumeCommand {
    UP,
    DOWN,
    MUTE
}

enum class HdmiPort(val rokuKey: String, val activeAppId: String, val displayName: String) {
    HDMI1("InputHDMI1", "tvinput.hdmi1", "HDMI 1"),
    HDMI2("InputHDMI2", "tvinput.hdmi2", "HDMI 2"),
    HDMI3("InputHDMI3", "tvinput.hdmi3", "HDMI 3"),
    HDMI4("InputHDMI4", "tvinput.hdmi4", "HDMI 4"),
    AV1("InputAV1", "tvinput.av1", "AV 1"),
    TUNER("InputTuner", "tvinput.dtv", "Tuner");

    companion object {
        fun fromName(value: String?): HdmiPort? =
            entries.firstOrNull { it.name == value }

        fun fromActiveAppId(value: String?): HdmiPort? =
            entries.firstOrNull { it.activeAppId.equals(value, ignoreCase = true) }
    }
}

sealed interface RemoteCommand {
    data class Dpad(val direction: DpadDirection) : RemoteCommand
    data object Select : RemoteCommand
    data object Home : RemoteCommand
    data object Back : RemoteCommand
    data object PlayPause : RemoteCommand
    data class Volume(val command: VolumeCommand) : RemoteCommand
    data object PowerOn : RemoteCommand
    data object PowerOff : RemoteCommand
    data object PowerToggle : RemoteCommand
    data class LaunchApp(val appIdOrUri: String) : RemoteCommand
    data class SetInput(val port: HdmiPort) : RemoteCommand
}

data class RegisteredDevice(
    val id: String,
    val type: DeviceType,
    val ipAddress: String,
    val port: Int,
    val uniqueId: String,
    val friendlyName: String,
    val lastSeenMillis: Long,
    val hdmiPortMapping: HdmiPort?,
    val isOnline: Boolean,
    val consecutiveFailures: Int
)

data class RokuApp(
    val id: String,
    val name: String,
    val type: String?
)

data class RokuDeviceInfo(
    val friendlyName: String?,
    val serialNumber: String?,
    val powerMode: String?,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val networkType: String? = null,
    val isTv: Boolean? = null,
    val supportsTvPowerControl: Boolean? = null,
    val supportsAudioVolumeControl: Boolean? = null
)

data class ActiveApp(
    val id: String,
    val name: String,
    val inferredHdmiPort: HdmiPort?
)

enum class AppTargetSource {
    DISCOVERED,
    PINNED,
    CUSTOM
}

data class AppLaunchTarget(
    val id: String,
    val deviceType: DeviceType,
    val displayName: String,
    val launchValue: String,
    val source: AppTargetSource
)

data class GoogleTvStatus(
    val isConnected: Boolean,
    val currentAppPackage: String?,
    val isOn: Boolean?
)

interface RemoteAdapter {
    suspend fun send(device: RegisteredDevice, command: RemoteCommand): Result<Unit>
}
