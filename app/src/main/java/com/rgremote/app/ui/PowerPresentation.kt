package com.rgremote.app.ui

import com.rgremote.app.domain.DeviceType
import com.rgremote.app.domain.RemoteCommand
import com.rgremote.app.roku.RokuPowerMode

/** What the power button should do right now, and how it should present. */
internal data class PowerPresentation(
    val command: RemoteCommand,
    /** True when the command wakes/keeps the TV on rather than cutting power. */
    val isWake: Boolean,
    /** True when Roku power mode is unknown and we are guessing. */
    val isUncertain: Boolean,
)

/**
 * Resolves the power action for the selected device.
 *
 * Roku never receives [RemoteCommand.PowerToggle]: `RokuEcpClient` rejects it.
 * When the power mode is unknown the safe fallback is [RemoteCommand.PowerOn]
 * (Wake-over-WLAN + Home), which is harmless if the TV is already on.
 *
 * Both standby (`displayoff`/`standby`) AND deep off (`poweroff`) resolve to
 * wake: deep-off includes Fast TV Start TVs whose ECP still answers while the
 * panel is dark, and the previous logic dispatched PowerOff at exactly the TV
 * that most needed waking.
 */
internal fun resolvePowerPresentation(
    ecosystemType: DeviceType,
    rokuPowerMode: String?,
    connectionStatus: ConnectionStatus,
): PowerPresentation {
    val shouldWake = RokuPowerMode.displayLabel(rokuPowerMode) == "Standby" ||
            !RokuPowerMode.isLikelyReachable(rokuPowerMode) ||
            connectionStatus == ConnectionStatus.OFFLINE
    return when {
        ecosystemType != DeviceType.ROKU_TV -> PowerPresentation(
            command = RemoteCommand.PowerToggle,
            isWake = false,
            isUncertain = false
        )
        // Unknown Roku power mode: dispatch wake, never PowerToggle.
        rokuPowerMode == null && connectionStatus != ConnectionStatus.OFFLINE -> PowerPresentation(
            command = RemoteCommand.PowerOn,
            isWake = true,
            isUncertain = true
        )
        shouldWake -> PowerPresentation(
            command = RemoteCommand.PowerOn,
            isWake = true,
            isUncertain = false
        )
        else -> PowerPresentation(
            command = RemoteCommand.PowerOff,
            isWake = false,
            isUncertain = false
        )
    }
}
