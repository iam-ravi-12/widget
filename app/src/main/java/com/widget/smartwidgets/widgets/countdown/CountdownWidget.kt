package com.widget.smartwidgets.widgets.countdown

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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import androidx.compose.ui.unit.sp

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val prefs = WidgetPreferences(context)
        val targetKey = PreferencesKeys.countdownTarget(appWidgetId)
        val titleKey = PreferencesKeys.countdownTitle(appWidgetId)
        
        val targetTimeStr = prefs.getPreference(targetKey, "").firstOrNull()
        val title = prefs.getPreference(titleKey, "").firstOrNull()
        
        val targetTime = targetTimeStr?.toLongOrNull() ?: 0L

        provideContent {
            val configIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartwidgets://countdown/config/$appWidgetId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.clickable(actionStartActivity(configIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (targetTime == 0L || title.isNullOrBlank()) {
                        Text(
                            text = "Countdown",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = "Tap to configure",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        val currentTime = System.currentTimeMillis()
                        val diffMillis = targetTime - currentTime
                        
                        Text(
                            text = title,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        if (diffMillis <= 0) {
                            Text(
                                text = "Completed!",
                                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold)
                            )
                        } else {
                            val remoteViews = android.widget.RemoteViews(context.packageName, com.widget.smartwidgets.R.layout.widget_countdown_timer)
                            val baseTime = android.os.SystemClock.elapsedRealtime() + diffMillis
                            remoteViews.setLong(com.widget.smartwidgets.R.id.chronometer, "setBase", baseTime)
                            remoteViews.setBoolean(com.widget.smartwidgets.R.id.chronometer, "setStarted", true)
                            androidx.glance.appwidget.AndroidRemoteViews(remoteViews)
                        }
                    }
                }
            }
        }
    }
}
