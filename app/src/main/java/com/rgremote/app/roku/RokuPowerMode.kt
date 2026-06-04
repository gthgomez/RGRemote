package com.rgremote.app.roku

object RokuPowerMode {
    fun displayLabel(raw: String?): String? =
        when (normalized(raw)) {
            "poweron" -> "On"
            "poweroff" -> "Off"
            "displayoff", "standby" -> "Standby"
            else -> raw?.trim()?.takeIf { it.isNotEmpty() }
        }

    /** False when ECP reports the TV is fully off and unlikely to answer LAN commands. */
    fun isLikelyReachable(raw: String?): Boolean =
        normalized(raw) != "poweroff"

    fun wakeHint(raw: String?): String? =
        when (normalized(raw)) {
            "poweroff" ->
                "TV appears fully off. Enable Fast TV Start on Roku for network wake."
            "displayoff", "standby" ->
                "TV in standby — Wake sends Home over the network."
            else -> null
        }

    private fun normalized(raw: String?): String? =
        raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}
