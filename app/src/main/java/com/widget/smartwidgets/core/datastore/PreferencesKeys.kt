package com.widget.smartwidgets.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Centralized DataStore preference keys.
 * Grouped by scope: global app settings and per-widget-type settings.
 */
object PreferencesKeys {
    // Global app preferences
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")

    // Per-widget-type preference key factories
    fun widgetRefreshInterval(widgetType: String) =
        intPreferencesKey("${widgetType}_refresh_interval_minutes")

    fun widgetEnabled(widgetType: String) =
        booleanPreferencesKey("${widgetType}_enabled")

    fun widgetTheme(widgetType: String) =
        stringPreferencesKey("${widgetType}_theme")
        
    fun weatherWidgetCity(widgetId: Int) =
        stringPreferencesKey("weather_widget_city_$widgetId")

    fun worldClockZones(widgetId: Int) =
        stringPreferencesKey("world_clock_zones_$widgetId")
        
    fun countdownTarget(widgetId: Int) =
        stringPreferencesKey("countdown_target_$widgetId")
        
    fun countdownTitle(widgetId: Int) =
        stringPreferencesKey("countdown_title_$widgetId")
        
    fun photoFrameUri(widgetId: Int) =
        stringPreferencesKey("photo_frame_uri_$widgetId")

    fun pomodoroState(widgetId: Int) =
        stringPreferencesKey("pomodoro_state_$widgetId")

    fun pomodoroEndTime(widgetId: Int) =
        stringPreferencesKey("pomodoro_end_time_$widgetId")
}
