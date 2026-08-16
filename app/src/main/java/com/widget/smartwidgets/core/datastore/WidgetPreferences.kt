package com.widget.smartwidgets.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance per process — top-level extension ensures one instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences"
)

/**
 * Lightweight preference access for widget settings.
 * Uses Jetpack DataStore instead of SharedPreferences for async, type-safe access.
 */
class WidgetPreferences(private val context: Context) {

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val use24HourFormat: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USE_24_HOUR_FORMAT] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun set24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USE_24_HOUR_FORMAT] = use24Hour
        }
    }

    /**
     * Read a preference value once (non-flowing).
     */
    fun <T> getPreference(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data.map { prefs -> prefs[key] ?: default }

    /**
     * Write a single preference value.
     */
    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }
}
