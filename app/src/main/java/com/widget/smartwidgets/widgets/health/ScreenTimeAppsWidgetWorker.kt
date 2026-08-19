package com.widget.smartwidgets.widgets.health

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScreenTimeAppsWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ScreenTimeAppsWidget::class.java)
            
            if (glanceIds.isEmpty()) {
                return Result.success()
            }

            val widget = ScreenTimeAppsWidget()
            glanceIds.forEach { id ->
                widget.update(context, id)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
