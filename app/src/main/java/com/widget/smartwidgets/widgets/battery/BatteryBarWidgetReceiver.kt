package com.widget.smartwidgets.widgets.battery

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryBarWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = BatteryBarWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BatteryMonitorServiceController.start(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BatteryMonitorServiceController.start(context)
    }

    override fun onDisabled(context: Context) {
        BatteryMonitorServiceController.stop(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        BatteryBarWidget().updateAll(context)
                    } catch (e: Exception) {
                        android.util.Log.e("BatteryBarReceiver", "Failed to refresh battery bar widgets", e)
                    }
                }
            }
        }
    }
}
