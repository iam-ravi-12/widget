package com.widget.smartwidgets.widgets.worldclock

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import kotlinx.coroutines.flow.firstOrNull
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class WorldClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorldClockWidget()
}

class WorldClockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val prefs = WidgetPreferences(context)
        val zonesKey = PreferencesKeys.worldClockZones(appWidgetId)
        val configuredZonesStr = prefs.getPreference(zonesKey, "").firstOrNull()
        val zones = if (configuredZonesStr.isNullOrBlank()) emptyList() else configuredZonesStr.split(",")

        provideContent {
            val configIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartwidgets://worldclock/config/$appWidgetId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            GlanceTheme {
                GlanceWidgetCard(modifier = GlanceModifier.clickable(actionStartActivity(configIntent))) {
                    Text(
                        text = "World Clock",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    if (zones.isEmpty()) {
                        Text(
                            text = "Tap to add cities",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        zones.take(4).forEach { zoneIdStr ->
                            val zoneId = try { ZoneId.of(zoneIdStr) } catch (e: Exception) { null }
                            if (zoneId != null) {
                                val time = ZonedDateTime.now(zoneId)
                                val timeStr = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                                val locationName = zoneIdStr.substringAfterLast("/").replace("_", " ")
                                
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().height(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = locationName,
                                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Text(
                                        text = timeStr,
                                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
