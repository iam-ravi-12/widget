package com.widget.smartwidgets.widgets.storage

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic WorkManager worker for storage usage updates.
 *
 * Runs every 60 minutes — storage usage changes slowly under normal conditions,
 * so a longer interval is appropriate to minimize battery impact.
 *
 * On each run, reads current storage via StatFs and updates all active
 * Storage widget instances.
 *
 * Lifecycle: Enqueued in StorageWidgetReceiver.onEnabled(), cancelled in onDisabled().
 */
class StorageWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(context)
            val widget = StorageWidget()
            val ids = manager.getGlanceIds(StorageWidget::class.java)
            ids.forEach { id ->
                widget.update(context, id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
