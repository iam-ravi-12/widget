package com.widget.smartwidgets.widgets.health

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.widget.smartwidgets.R
import com.widget.smartwidgets.utils.UsageAccessUtil
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScreenTimeAppsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeAppsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val workRequest = PeriodicWorkRequestBuilder<ScreenTimeAppsWidgetWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ScreenTimeAppsWidgetWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("ScreenTimeAppsWidgetWorker")
    }
}

class RefreshScreenTimeAppsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ScreenTimeAppsWidget().update(context, glanceId)
    }
}

class ScreenTimeAppsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasPermission = UsageAccessUtil.hasUsageStatsPermission(context)
        var totalUsageStr = "0h 0m"
        val topAppBitmaps = mutableListOf<Bitmap>()

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            // --- 1. MAIN TOTAL ---
            var totalInteractiveMs = 0L
            var lastInteractiveTime = 0L
            var isScreenInteractive = false

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
                        val duration = event.timeStamp - Math.max(lastInteractiveTime, startTime)
                        if (duration > 0) {
                            totalInteractiveMs += duration
                        }
                    }
                }
            }

            if (isScreenInteractive) {
                val duration = endTime - Math.max(lastInteractiveTime, startTime)
                if (duration > 0) {
                    totalInteractiveMs += duration
                }
            }

            if (totalInteractiveMs > 0) {
                val hours = (totalInteractiveMs / (1000 * 60 * 60))
                val minutes = (totalInteractiveMs / (1000 * 60)) % 60
                val formattedMinutes = String.format(Locale.getDefault(), "%02d", minutes)
                totalUsageStr = if (hours > 0) "${hours}h\u00A0${formattedMinutes}m" else "${minutes}m"
            } else {
                totalUsageStr = "0m"
            }

            // --- 2. TOP APPS (Event-derived for comparison) ---
            val packageTimeMapEvent = mutableMapOf<String, Long>()
            val packageForegroundStart = mutableMapOf<String, Long>()

            val appEvents = usageStatsManager.queryEvents(startTime, endTime)
            val appEvent = android.app.usage.UsageEvents.Event()

            while (appEvents.hasNextEvent()) {
                appEvents.getNextEvent(appEvent)
                val pkg = appEvent.packageName
                val timeStamp = appEvent.timeStamp
                
                // ACTIVITY_RESUMED (1)
                if (appEvent.eventType == 1) { 
                    packageForegroundStart[pkg] = timeStamp
                } 
                // ACTIVITY_PAUSED (2)
                else if (appEvent.eventType == 2) {
                    val start = packageForegroundStart.remove(pkg)
                    if (start != null) {
                        val duration = timeStamp - start
                        if (duration > 0) {
                            packageTimeMapEvent[pkg] = (packageTimeMapEvent[pkg] ?: 0L) + duration
                        }
                    }
                }
            }

            // 5. CURRENTLY ACTIVE APP
            for ((pkg, start) in packageForegroundStart) {
                val duration = endTime - start
                if (duration > 0) {
                    packageTimeMapEvent[pkg] = (packageTimeMapEvent[pkg] ?: 0L) + duration
                }
            }

            // --- 3. TOP APPS (Aggregated Stats - More Accurate) ---
            val packageTimeMap = mutableMapOf<String, Long>()
            val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            for ((pkg, stats) in aggregatedStats) {
                if (stats.totalTimeInForeground > 0) {
                    packageTimeMap[pkg] = stats.totalTimeInForeground
                }
            }

            val pm = context.packageManager
            
            val eligibleApps = mutableListOf<Pair<String, Long>>()
            val fallbackApps = mutableListOf<Pair<String, Long>>()

            var rawUsageStatsCount = packageTimeMap.size
            var packagesWithUsage = 0
            var systemFilteredApps = 0
            
            for ((pkg, fgTime) in packageTimeMap) {
                val eventTime = packageTimeMapEvent[pkg] ?: 0L
                if (fgTime < 1000L && eventTime < 1000L) continue // Remove zero/near-zero usage
                packagesWithUsage++

                var label = pkg
                var excludedReason: String? = null
                
                try {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    label = pm.getApplicationLabel(ai).toString()
                    val isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    val hasLauncherActivity = launchIntent != null
                    
                    val isSystemUI = pkg == "com.android.systemui" || pkg == "com.android.settings" || pkg == context.packageName
                    val isLauncher = isLauncherApp(context, pkg)
                    
                    val isCoreSystem = pkg == "android" || pkg.startsWith("android.") || pkg.startsWith("com.android.providers.")
                    
                    // Add to fallback immediately if it's not obvious system UI/launcher
                    if (!isSystemUI && !isLauncher && !isCoreSystem) {
                        fallbackApps.add(Pair(pkg, fgTime))
                    }

                    if (isSystemUI) {
                        excludedReason = "system_ui_or_self"
                    } else if (isLauncher) {
                        excludedReason = "launcher"
                    } else if (isCoreSystem) {
                        excludedReason = "core_system"
                    } else if (!hasLauncherActivity) {
                        // Strict filter: require a launcher activity
                        excludedReason = "not_launchable"
                    }

                    android.util.Log.d("TOP_APPS_DEBUG", "package=$pkg label=$label usageStatsMs=$fgTime eventMs=$eventTime isSystemApp=$isSystemApp isUpdatedSystemApp=$isUpdatedSystemApp hasLauncherActivity=$hasLauncherActivity excluded=${excludedReason != null} excludedReason=${excludedReason ?: "none"}")

                    if (excludedReason == null) {
                        // Prefer aggregated stats for ranking, as it handles midnight boundary correctly
                        eligibleApps.add(Pair(pkg, maxOf(fgTime, eventTime)))
                    } else {
                        systemFilteredApps++
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    android.util.Log.d("TOP_APPS_DEBUG", "package=$pkg error=NotFound (likely due to missing QUERY_ALL_PACKAGES previously)")
                }
            }

            android.util.Log.d("TOP_APPS_DEBUG", "rawUsageStatsCount = $rawUsageStatsCount")
            android.util.Log.d("TOP_APPS_DEBUG", "packagesWithUsage = $packagesWithUsage")
            android.util.Log.d("TOP_APPS_DEBUG", "systemFilteredApps = $systemFilteredApps")
            android.util.Log.d("TOP_APPS_DEBUG", "finalEligibleApps = ${eligibleApps.size}")

            var sortedEligibleApps = eligibleApps.sortedByDescending { it.second }

            if (sortedEligibleApps.isEmpty() && fallbackApps.isNotEmpty()) {
                android.util.Log.d("TOP_APPS_DEBUG", "TOP_APPS_EMPTY: Main filtering returned 0 apps. Using fallback apps list.")
                sortedEligibleApps = fallbackApps.sortedByDescending { it.second }
            }

            var count = 0
            for ((pkg, fgTime) in sortedEligibleApps) {
                if (count >= 8) break
                try {
                    val iconDrawable = pm.getApplicationIcon(pkg)
                    val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    android.util.Log.d("TOP_APPS_FINAL", "rank=${count + 1} package=$pkg label=$label foregroundMs=$fgTime")
                    topAppBitmaps.add(drawableToBitmap(iconDrawable))
                    count++
                } catch (e: Exception) {
                    android.util.Log.d("TOP_APPS_DEBUG", "Error loading icon for package=$pkg")
                    // Do not discard the entire list if one icon fails.
                }
            }
            android.util.Log.d("TOP_APPS_DEBUG", "finalTopApps = ${topAppBitmaps.size}")
        }

        provideContent {
            val size = LocalSize.current
            val widgetHeight = size.height

            val isSmall = widgetHeight < 130.dp
            val isVerySmall = widgetHeight < 110.dp

            val paddingHoriz = 4.dp
            val paddingVert = 4.dp
            val totalTimeTextSize = if (isSmall) 24.sp else 26.sp
            val phoneIconSize = if (isSmall) 24.dp else 28.dp

            val maxIcons = when {
                isVerySmall -> 4
                isSmall -> 6
                else -> 8
            }
            val firstRowCount = if (isVerySmall) 4 else if (isSmall) 3 else 4

            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.clickable(
                        if (!hasPermission) {
                            actionStartActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } else {
                            actionRunCallback<RefreshScreenTimeAppsAction>()
                        }
                    ),
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!hasPermission) {
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Usage access required",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Tap to enable",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else {
                        Column(
                            modifier = GlanceModifier.fillMaxSize()
                                .padding(horizontal = paddingHoriz, vertical = paddingVert)
                        ) {
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = "TOTAL TIME",
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    Text(
                                        text = totalUsageStr,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = totalTimeTextSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = GlanceModifier.padding(start = 4.dp)
                                ) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_mobile_phone),
                                        contentDescription = "Phone",
                                        modifier = GlanceModifier.size(phoneIconSize).padding(top = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            // App Icons Layout
                            if (topAppBitmaps.isNotEmpty()) {
                                Column(modifier = GlanceModifier.fillMaxWidth()) {
                                    // First Row
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth()
                                            .padding(bottom = if (isSmall) 4.dp else 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 0 until minOf(firstRowCount, minOf(maxIcons, topAppBitmaps.size))) {
                                            val sizeIcon =
                                                if (i == 0 || i == 2) (if (isSmall) 26.dp else 30.dp) else (if (isSmall) 24.dp else 26.dp)
                                            Image(
                                                provider = ImageProvider(topAppBitmaps[i]),
                                                contentDescription = "App",
                                                modifier = GlanceModifier.size(sizeIcon).padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                    // Second Row
                                    if (topAppBitmaps.size > firstRowCount && maxIcons > firstRowCount) {
                                        Row(
                                            modifier = GlanceModifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (i in firstRowCount until minOf(maxIcons, topAppBitmaps.size)) {
                                                val sizeIcon =
                                                    if (i == firstRowCount + 1) (if (isSmall) 26.dp else 28.dp) else (if (isSmall) 24.dp else 26.dp)
                                                Image(
                                                    provider = ImageProvider(topAppBitmaps[i]),
                                                    contentDescription = "App",
                                                    modifier = GlanceModifier.size(sizeIcon).padding(horizontal = 4.dp)
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
        }
    }

    private fun isLauncherApp(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
