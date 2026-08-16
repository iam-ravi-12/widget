package com.widget.smartwidgets.widgets.clock

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.util.Log
import com.widget.smartwidgets.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WIDGET_TEST", "onUpdate called for IDs: ${appWidgetIds.joinToString()}")
        
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_test)
            views.setTextViewText(R.id.test_text, timestamp)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
