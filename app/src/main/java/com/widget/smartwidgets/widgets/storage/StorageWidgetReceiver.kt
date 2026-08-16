package com.widget.smartwidgets.widgets.storage

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
 * BroadcastReceiver for the Storage widget.
 *
 * Update architecture:
 *
 * There is no broadcast for individual storage usage changes on modern Android.
 * Storage values change slowly under normal conditions (app installs, media
 * downloads, cache growth).
 *
 * Therefore, the Storage widget uses a conservative periodic strategy:
 *
 * 1. PERIODIC WORKMANAGER (60 min):
 *    Storage changes slowly, so a 60-minute interval balances usefulness
 *    with battery efficiency.
 *
 * 2. APPWIDGET_UPDATE (30 min via updatePeriodMillis):
 *    System-level fallback. Always reads current StatFs state.
 *
 * Lifecycle:
 * - onUpdate() → ensure WorkManager periodic task is enqueued
 * - onDisabled() → cancel WorkManager periodic task
 */
class StorageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StorageWidget()
}
