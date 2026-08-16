package com.widget.smartwidgets.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey
    val locationId: String, // e.g., "london:metric" or "51.51,-0.13:imperial"
    val displayName: String,
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val iconCode: String,
    val fetchedAt: Long
)
