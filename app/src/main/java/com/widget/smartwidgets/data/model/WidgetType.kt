package com.widget.smartwidgets.data.model

/**
 * Registry of available widget types.
 * Each widget type has display metadata and a description for the main app UI.
 */
enum class WidgetType(val displayName: String, val description: String) {
    CLOCK(
        displayName = "Clock",
        description = "Shows current time and date"
    ),
    BATTERY(
        displayName = "Battery",
        description = "Battery percentage and charging state"
    ),
    // Future widget types — uncomment and implement as needed:
    // WEATHER("Weather", "Current weather conditions"),
    // QUOTES("Daily Quote", "Inspirational quotes, refreshed periodically"),
    // RSS("RSS Feed", "Latest headlines from your favorite feeds"),
}
