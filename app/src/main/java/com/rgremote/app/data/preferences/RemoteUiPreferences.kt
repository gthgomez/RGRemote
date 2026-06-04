package com.rgremote.app.data.preferences

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RemoteUiPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _showConnectionGuide = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GUIDE, true))
    val showConnectionGuide: StateFlow<Boolean> = _showConnectionGuide

    private val _showUtilitiesDock = MutableStateFlow(prefs.getBoolean(KEY_SHOW_UTILITIES, true))
    val showUtilitiesDock: StateFlow<Boolean> = _showUtilitiesDock

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
    }
}
