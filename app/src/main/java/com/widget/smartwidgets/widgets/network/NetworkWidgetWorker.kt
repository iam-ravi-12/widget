package com.widget.smartwidgets.widgets.network

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic WorkManager worker for network connectivity updates.
 *
 * Acts as a fallback alongside event-driven CONNECTIVITY_CHANGE broadcasts.
 * Runs every 30 minutes to ensure the widget reflects current connectivity
 * even if a broadcast was missed or delayed.
 *
 * Lifecycle: Enqueued in NetworkWidgetReceiver.onEnabled(), cancelled in onDisabled().
 */
class NetworkWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(context)
            val widget = NetworkWidget()
            val ids = manager.getGlanceIds(NetworkWidget::class.java)
            ids.forEach { id ->
                widget.update(context, id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
