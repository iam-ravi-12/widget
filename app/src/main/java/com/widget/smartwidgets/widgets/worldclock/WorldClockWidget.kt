package com.widget.smartwidgets.widgets.worldclock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.R
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import java.util.Locale

class WorldClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorldClockWidget()
}

class WorldClockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)
        val prefs = WidgetPreferences(context)
        val zonesKey = PreferencesKeys.worldClockZones(appWidgetId)
        val configuredZonesStr = prefs.getPreference(zonesKey, "").kotlinx.coroutines.flow.firstOrNull()
        val zones = if (configuredZonesStr.isNullOrBlank()) emptyList() else configuredZonesStr.split(",").filter { it.isNotBlank() }.take(4)

        provideContent {
            val configIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartwidgets://worldclock/config/$appWidgetId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(configIntent))
                ) {
                    if (zones.isEmpty()) {
                        WorldClockEmptyState()
                    } else {
                        WorldClockRemoteViews(context, zones)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldClockEmptyState() {
    WorldClockRemoteViews(
        context = androidx.glance.LocalContext.current,
        zones = emptyList()
    )
}

@Composable
private fun WorldClockRemoteViews(context: Context, zones: List<String>) {
    val views = RemoteViews(context.packageName, R.layout.widget_world_clock_layout)
    val rowIds = intArrayOf(
        R.id.world_clock_row_1,
        R.id.world_clock_row_2,
        R.id.world_clock_row_3,
        R.id.world_clock_row_4
    )
    val cityIds = intArrayOf(
        R.id.world_clock_city_1,
        R.id.world_clock_city_2,
        R.id.world_clock_city_3,
        R.id.world_clock_city_4
    )
    val timeIds = intArrayOf(
        R.id.world_clock_time_1,
        R.id.world_clock_time_2,
        R.id.world_clock_time_3,
        R.id.world_clock_time_4
    )

    for (i in rowIds.indices) {
        if (i < zones.size) {
            val zoneId = zones[i]
            views.setViewVisibility(rowIds[i], View.VISIBLE)
            views.setTextViewText(cityIds[i], friendlyZoneName(zoneId))
            // TextClock updates itself from the Android system clock; Kotlin does not
            // calculate a snapshot time here.
            views.setString(timeIds[i], "setTimeZone", zoneId)
        } else {
            views.setViewVisibility(rowIds[i], View.GONE)
        }
    }

    if (zones.isEmpty()) {
        views.setTextViewText(R.id.world_clock_city_1, "Tap to add cities")
        views.setViewVisibility(R.id.world_clock_row_1, View.VISIBLE)
        views.setViewVisibility(R.id.world_clock_time_1, View.GONE)
    } else {
        views.setViewVisibility(R.id.world_clock_time_1, View.VISIBLE)
    }

    AndroidRemoteViews(views)
}

private fun friendlyZoneName(zoneId: String): String {
    val lastPart = zoneId.substringAfterLast('/').replace('_', ' ')
    return when {
        lastPart.equals("UTC", ignoreCase = true) -> "UTC"
        lastPart.isBlank() -> zoneId
        else -> lastPart.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}
