package com.widget.smartwidgets.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_config")
data class WeatherWidgetConfigEntity(
    @PrimaryKey
    val appWidgetId: Int,
    val locationMode: String, // "CURRENT_LOCATION" or "MANUAL_CITY"
    val cityName: String,
    val latitude: Double?,
    val longitude: Double?,
    val temperatureUnit: String // "CELSIUS" or "FAHRENHEIT"
)
