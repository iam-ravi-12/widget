package com.widget.smartwidgets.widgets.memory

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.widget.smartwidgets.core.worker.WorkerConstants
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver for the Memory/RAM widget.
 *
 * Update architecture:
 * - There is no reliable universal Android broadcast for "RAM usage changed."
 * - A 30-min WorkManager periodic task reads ActivityManager.MemoryInfo and
 *   updates all widget instances.
 * - APPWIDGET_UPDATE fires periodically (every 30 min via updatePeriodMillis)
 *   as an additional system-level fallback.
 *
 * This is NOT a real-time RAM monitor — it provides periodic snapshots of
 * memory usage. This is the correct trade-off between useful information
 * and battery efficiency.
 *
 * Lifecycle:
 * - onUpdate() → ensure WorkManager periodic task is enqueued
 * - onDisabled() → cancel WorkManager periodic task
 */
class MemoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MemoryWidget()
}
