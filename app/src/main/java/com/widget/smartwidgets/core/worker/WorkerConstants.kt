package com.widget.smartwidgets.core.worker

/**
 * Constants for WorkManager scheduling.
 *
 * Keep intervals as long as acceptable to minimize battery impact.
 * WorkManager's minimum periodic interval is 15 minutes.
 */
object WorkerConstants {
    const val MIN_PERIODIC_INTERVAL_MINUTES = 15L

    // System-information widget update intervals.
    //
    // On Android 8.0+ (minSdk=26), POWER_CONNECTED/DISCONNECTED, BATTERY_LOW/OKAY,
    // CONNECTIVITY_CHANGE, and DEVICE_STORAGE_LOW/OK are NOT exempt from implicit
    // broadcast restrictions. Manifest-registered receivers will NOT receive them.
    // WorkManager periodic tasks are the primary update mechanism.

    // Battery: 15 min (WorkManager minimum). Charging-state reactivity is provided
    // by constraint-based one-time workers (requiresCharging=true/false).
    const val BATTERY_UPDATE_INTERVAL_MINUTES = 15L

    // Network: 15 min (WorkManager minimum). Network state can change frequently
    // and users expect reasonable freshness.
    const val NETWORK_UPDATE_INTERVAL_MINUTES = 15L

    // Storage: 60 min — storage usage changes slowly under normal conditions.
    const val STORAGE_UPDATE_INTERVAL_MINUTES = 60L

    // Memory: 30 min — no reliable Android broadcast for RAM usage changes,
    // so periodic refresh is the only sensible approach.
    const val MEMORY_UPDATE_INTERVAL_MINUTES = 30L

    // Content/data widget intervals
    const val WEATHER_UPDATE_INTERVAL_MINUTES = 60L
    const val NEWS_UPDATE_INTERVAL_MINUTES = 30L
    const val QUOTES_UPDATE_INTERVAL_MINUTES = 360L // 6 hours

    // Work tags for identifying and managing scheduled work
    const val TAG_WIDGET_UPDATE = "widget_update"

    fun tagForWidget(widgetType: String) = "widget_update_$widgetType"
}
