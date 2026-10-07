package com.rgremote.app.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.rgremote.app.ui.RemoteTab
import com.rgremote.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RemoteUiPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _showConnectionGuide = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GUIDE, true))
    val showConnectionGuide: StateFlow<Boolean> = _showConnectionGuide

    private val _showUtilitiesDock = MutableStateFlow(prefs.getBoolean(KEY_SHOW_UTILITIES, true))
    val showUtilitiesDock: StateFlow<Boolean> = _showUtilitiesDock

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, null)?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private val startupTabInitial: RemoteTab? = prefs.getString(KEY_STARTUP_TAB, null)?.let { name ->
        RemoteTab.entries.firstOrNull { it.name == name }
    }

    val selectedDeviceId: String?
        get() = prefs.getString(KEY_SELECTED_DEVICE_ID, null)

    fun setSelectedDeviceId(deviceId: String?) {
        prefs.edit { putString(KEY_SELECTED_DEVICE_ID, deviceId) }
    }

    val selectedTab: RemoteTab?
        get() = prefs.getString(KEY_SELECTED_TAB, null)?.let { name ->
            RemoteTab.entries.firstOrNull { it.name == name }
        }

    fun setSelectedTab(tab: RemoteTab?) {
        prefs.edit { putString(KEY_SELECTED_TAB, tab?.name) }
    }

    private val _startupTab = MutableStateFlow(startupTabInitial)
    val startupTab: StateFlow<RemoteTab?> = _startupTab

    fun setStartupTab(tab: RemoteTab?) {
        prefs.edit { putString(KEY_STARTUP_TAB, tab?.name) }
        _startupTab.value = tab
    }

    fun setShowConnectionGuide(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_GUIDE, show) }
        _showConnectionGuide.value = show
    }

    fun setShowUtilitiesDock(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_UTILITIES, show) }
        _showUtilitiesDock.value = show
    }

    companion object {
        private const val PREFS_NAME = "rgremote_ui_prefs"
        private const val KEY_SHOW_GUIDE = "show_connection_guide"
        private const val KEY_SHOW_UTILITIES = "show_utilities_dock"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SELECTED_DEVICE_ID = "selected_device_id"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_STARTUP_TAB = "startup_tab"
    }
}
