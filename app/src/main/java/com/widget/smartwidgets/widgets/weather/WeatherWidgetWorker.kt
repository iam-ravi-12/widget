package com.widget.smartwidgets.widgets.weather

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkerParameters
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.core.worker.BaseWidgetWorker
import com.widget.smartwidgets.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic WorkManager worker for weather updates.
 */
class WeatherWidgetWorker(
    private val context: Context,
    params: WorkerParameters
) : BaseWidgetWorker(context, params) {

    override suspend fun fetchData(): Result {
        Log.d(TAG, "Fetching periodic weather data...")
        return withContext(Dispatchers.IO) {
            try {
                val activeWidgetIds = WeatherWidgetReceiver.getActiveWidgetIds(context)
                if (activeWidgetIds.isEmpty()) {
                    Log.d(TAG, "No active weather widgets, success without fetch")
                    return@withContext Result.success()
                }

                val db = AppDatabase.getInstance(context)
                val repository = WeatherRepository.getInstance(context)
                var anyUpdate = false

                for (appWidgetId in activeWidgetIds) {
                    val config = db.weatherWidgetConfigDao().getConfig(appWidgetId) ?: continue
                    
                    try {
                        if (config.locationMode == "CURRENT_LOCATION" && config.latitude != null && config.longitude != null) {
                            repository.getWeatherForCoordinates(config.latitude, config.longitude, config.temperatureUnit, forceRefresh = true)
                            anyUpdate = true
                        } else if (config.cityName.isNotBlank()) {
                            repository.getWeatherForCity(config.cityName, config.temperatureUnit, forceRefresh = true)
                            anyUpdate = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed updating weather for widget $appWidgetId", e)
                    }
                }

                if (anyUpdate) {
                    val manager = GlanceAppWidgetManager(context)
                    for (appWidgetId in activeWidgetIds) {
                        try {
                            val glanceId = manager.getGlanceIdBy(appWidgetId)
                            WeatherWidget().update(context, glanceId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update Glance widget $appWidgetId", e)
                        }
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error in WeatherWidgetWorker", e)
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "WeatherWidgetWorker"
    }
}
