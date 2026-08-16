package com.widget.smartwidgets.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Base worker for widget data refresh tasks.
 *
 * Subclass this for widgets that need periodic background data fetching
 * (e.g., weather, RSS feeds, quotes). The Clock widget does NOT need this
 * because TextClock renders the system clock directly.
 *
 * Usage pattern:
 * 1. Create a subclass overriding fetchData()
 * 2. Schedule via WorkManager with appropriate constraints
 * 3. After fetchData() succeeds, trigger a widget update via GlanceAppWidget.update()
 */
abstract class BaseWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    abstract suspend fun fetchData(): Result

    override suspend fun doWork(): Result {
        return try {
            fetchData()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
