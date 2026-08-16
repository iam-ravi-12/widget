package com.widget.smartwidgets.widgets.weather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.widget.smartwidgets.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

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
    }
}
