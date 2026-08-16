package com.widget.smartwidgets.widgets.weather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.core.worker.WorkerConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWeatherWorker(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleWeatherWorker(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled: Canceling WeatherWorker")
        WorkManager.getInstance(context).cancelUniqueWork(WorkerConstants.tagForWidget("weather"))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "onDeleted received for widgetIds: ${appWidgetIds.joinToString()}")
        val db = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    db.weatherWidgetConfigDao().deleteConfig(id)
                    Log.d(TAG, "Cleaned up config from Room for deleted widgetId: $id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up configs for deleted widgets", e)
            }
        }
    }

    companion object {
        private const val TAG = "WeatherWidgetReceiver"

        fun getActiveWidgetIds(context: Context): IntArray {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeatherWidgetReceiver::class.java)
            return appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf()
        }

        fun scheduleWeatherWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(
                WorkerConstants.WEATHER_UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WorkerConstants.tagForWidget("weather"),
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Scheduled WeatherWidgetWorker")
        }
    }
}
