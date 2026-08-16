package com.widget.smartwidgets.widgets.memory

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic WorkManager worker for memory/RAM usage updates.
 *
 * Runs every 30 minutes. On each run, reads current RAM usage via
 * ActivityManager.MemoryInfo and updates all active Memory widget instances.
 *
 * There is no reliable Android broadcast for RAM changes, so periodic refresh
 * is the only sensible approach. 30 minutes is a reasonable interval that
 * balances usefulness with battery efficiency.
 *
 * Lifecycle: Enqueued in MemoryWidgetReceiver.onEnabled(), cancelled in onDisabled().
 */
class MemoryWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(context)
            val widget = MemoryWidget()
            val ids = manager.getGlanceIds(MemoryWidget::class.java)
            ids.forEach { id ->
                widget.update(context, id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
