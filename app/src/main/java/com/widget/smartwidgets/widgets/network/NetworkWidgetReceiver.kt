package com.widget.smartwidgets.widgets.network

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
 * BroadcastReceiver for the Network widget.
 *
 * Update architecture:
 *
 * On modern Android (8.0+), CONNECTIVITY_CHANGE is NOT exempt from implicit
 * broadcast restrictions. Manifest-registered receivers will NOT receive it.
 *
 * Therefore, the Network widget uses a dual-layered update strategy:
 *
 * 1. PERIODIC WORKMANAGER (15 min):
 *    Runs every 15 minutes. On each run, reads ConnectivityManager for
 *    current network state and updates all widget instances.
 *
 * 2. APPWIDGET_UPDATE (30 min via updatePeriodMillis):
 *    System-level fallback. Always reads current ConnectivityManager state.
 *
 * Lifecycle:
 * - onUpdate() → ensure WorkManager periodic task is enqueued
 * - onDisabled() → cancel WorkManager periodic task
 */
class NetworkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NetworkWidget()
}
