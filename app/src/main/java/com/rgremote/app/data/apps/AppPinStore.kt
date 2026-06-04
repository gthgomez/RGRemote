package com.rgremote.app.data.apps

import android.content.Context
import androidx.core.content.edit
import com.rgremote.app.domain.AppLaunchTarget
import com.rgremote.app.domain.AppTargetSource
import com.rgremote.app.domain.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppPinStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _pinnedApps = MutableStateFlow(loadPins())
    val pinnedApps: StateFlow<List<AppLaunchTarget>> = _pinnedApps

    fun upsert(target: AppLaunchTarget) {
        val normalized = target.normalized()
        val next = _pinnedApps.value.asSequence()
            .filterNot { it.id == normalized.id }
            .plus(normalized)
            .sortedWith(compareBy<AppLaunchTarget> { it.deviceType.name }.thenBy { it.displayName.lowercase() })
            .toList()
        save(next)
    }

    fun remove(targetId: String) {
        save(_pinnedApps.value.filterNot { it.id == targetId })
    }

    fun resetDefaults() {
        save(defaultPins())
    }

    private fun loadPins(): List<AppLaunchTarget> {
        val raw = prefs.getString(KEY_PINS, null) ?: return defaultPins()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val deviceType = runCatching { DeviceType.valueOf(item.getString("deviceType")) }.getOrNull()
                    val source = runCatching { AppTargetSource.valueOf(item.getString("source")) }.getOrNull()
                    val displayName = item.optString("displayName").trim()
                    val launchValue = item.optString("launchValue").trim()
                    if ((deviceType != null) && (source != null) && displayName.isNotBlank() && launchValue.isNotBlank()) {
                        add(
                            AppLaunchTarget(
                                id = item.optString("id").ifBlank { targetId(deviceType, launchValue) },
                                deviceType = deviceType,
                                displayName = displayName,
                                launchValue = launchValue,
                                source = source,
                            ).normalized(),
                        )
                    }
                }
            }
        }.getOrElse { defaultPins() }
    }

    private fun save(targets: List<AppLaunchTarget>) {
        _pinnedApps.value = targets
        val array = JSONArray()
        targets.forEach { target ->
            array.put(
                JSONObject()
                    .put("id", target.id)
                    .put("deviceType", target.deviceType.name)
                    .put("displayName", target.displayName)
                    .put("launchValue", target.launchValue)
                    .put("source", target.source.name),
            )
        }
        prefs.edit { putString(KEY_PINS, array.toString()) }
    }

    private fun AppLaunchTarget.normalized(): AppLaunchTarget {
        val value = launchValue.trim()
        return copy(
            id = targetId(deviceType, value),
            displayName = displayName.trim(),
            launchValue = value,
        )
    }

    private fun defaultPins(): List<AppLaunchTarget> =
        listOf(
            AppLaunchTarget(
                id = targetId(DeviceType.GOOGLE_TV, "com.google.android.youtube.tv"),
                deviceType = DeviceType.GOOGLE_TV,
                displayName = "YouTube",
                launchValue = "com.google.android.youtube.tv",
                source = AppTargetSource.PINNED,
            ),
            AppLaunchTarget(
                id = targetId(DeviceType.GOOGLE_TV, "com.netflix.ninja"),
                deviceType = DeviceType.GOOGLE_TV,
                displayName = "Netflix",
                launchValue = "com.netflix.ninja",
                source = AppTargetSource.PINNED,
            ),
            AppLaunchTarget(
                id = targetId(DeviceType.GOOGLE_TV, "com.disney.disneyplus"),
                deviceType = DeviceType.GOOGLE_TV,
                displayName = "Disney+",
                launchValue = "com.disney.disneyplus",
                source = AppTargetSource.PINNED,
            ),
            AppLaunchTarget(
                id = targetId(DeviceType.GOOGLE_TV, "com.plexapp.android"),
                deviceType = DeviceType.GOOGLE_TV,
                displayName = "Plex",
                launchValue = "com.plexapp.android",
                source = AppTargetSource.PINNED,
            ),
        )

    companion object {
        private const val PREFS_NAME = "rgremote_app_pins"
        private const val KEY_PINS = "pins"

        fun targetId(deviceType: DeviceType, launchValue: String): String =
            "${deviceType.name}:${launchValue.trim().lowercase()}"
    }
}
