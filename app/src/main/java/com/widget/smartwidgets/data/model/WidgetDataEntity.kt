package com.widget.smartwidgets.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic cache entity for widget data.
 *
 * Future widgets store fetched data here (weather, RSS items, quotes, etc.)
 * to avoid unnecessary network requests. The Clock widget does not use this
 * table — it reads the system clock directly via TextClock.
 */
@Entity(tableName = "widget_data")
data class WidgetDataEntity(
    @PrimaryKey
    val id: String,
    val widgetType: String,
    val data: String, // JSON string of cached data
    val lastUpdated: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L
)
