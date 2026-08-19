package com.widget.smartwidgets.widgets.health

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import java.util.Calendar
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionRunCallback
import com.widget.smartwidgets.utils.UsageAccessUtil

class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val workRequest = PeriodicWorkRequestBuilder<ScreenTimeWidgetWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ScreenTimeWidgetWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("ScreenTimeWidgetWorker")
    }
}

class RefreshScreenTimeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ScreenTimeWidget().update(context, glanceId)
    }
}

class ScreenTimeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasPermission = UsageAccessUtil.hasUsageStatsPermission(context)
        var totalUsageStr = "0m"
        var topAppName: String? = null
        var topAppUsageStr: String? = null

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            // --- 1. TOP APP ---
            val usageEventsStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            var maxTimeMs = 0L
            var maxPackage = ""
            
            usageEventsStats?.forEach { stats ->
                val fgTime = stats.totalTimeInForeground
                if (fgTime > maxTimeMs) {
                    maxTimeMs = fgTime
                    maxPackage = stats.packageName
                }
            }

            // --- 2. MAIN TOTAL ---
            var totalInteractiveMs = 0L
            var lastInteractiveTime = 0L
            var lastNonInteractiveTime = 0L
            var isScreenInteractive = false
            var intervalsCount = 0

            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = android.app.usage.UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE) {
                    if (!isScreenInteractive) {
                        isScreenInteractive = true
                        lastInteractiveTime = event.timeStamp
                    }
                } else if (event.eventType == android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                    if (isScreenInteractive) {
                        isScreenInteractive = false
                        lastNonInteractiveTime = event.timeStamp
                        val duration = event.timeStamp - Math.max(lastInteractiveTime, startTime)
                        if (duration > 0) {
                            totalInteractiveMs += duration
                            intervalsCount++
                        }
                    }
                }
            }

            if (isScreenInteractive) {
                val duration = endTime - Math.max(lastInteractiveTime, startTime)
                if (duration > 0) {
                    totalInteractiveMs += duration
                    intervalsCount++
                }
            }

            if (totalInteractiveMs > 0) {
                val hours = (totalInteractiveMs / (1000 * 60 * 60))
                val minutes = (totalInteractiveMs / (1000 * 60)) % 60
                totalUsageStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            }

            android.util.Log.d("SCREEN_TIME_DEBUG", "SCREEN_TIME_DEBUG\n" +
                "todayStart = $startTime\n" +
                "now = $endTime\n" +
                "interactiveIntervals = $intervalsCount\n" +
                "totalInteractiveMs = $totalInteractiveMs\n" +
                "formattedTotal = $totalUsageStr\n" +
                "lastScreenInteractive = $lastInteractiveTime\n" +
                "lastScreenNonInteractive = $lastNonInteractiveTime\n" +
                "screenCurrentlyInteractive = $isScreenInteractive"
            )

            if (maxTimeMs > 0 && maxPackage.isNotEmpty()) {
                val hours = (maxTimeMs / (1000 * 60 * 60))
                val minutes = (maxTimeMs / (1000 * 60)) % 60
                topAppUsageStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                
                try {
                    val pm = context.packageManager
                    val ai = pm.getApplicationInfo(maxPackage, 0)
                    topAppName = pm.getApplicationLabel(ai).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    topAppName = maxPackage
                }
            }
        }

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.clickable(
                        if (!hasPermission) {
                            actionStartActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } else {
                            actionRunCallback<RefreshScreenTimeAction>()
                        }
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screen Time",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (!hasPermission) {
                        Text(
                            text = "Usage access required",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to enable",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    } else {
                        Text(
                            text = totalUsageStr,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Today",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        
                        if (topAppName != null && topAppUsageStr != null) {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = "Top App",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Text(
                                text = "$topAppName · $topAppUsageStr",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
