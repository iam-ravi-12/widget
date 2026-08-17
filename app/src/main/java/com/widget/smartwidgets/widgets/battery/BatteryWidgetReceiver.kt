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

/**
 * AppWidget receiver for Battery widgets.
 * A foreground service keeps a dynamic BATTERY_CHANGED listener alive while
 * at least one Battery widget exists.
 */
class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()

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
                        BatteryWidget().updateAll(context)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed to refresh battery widgets", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BatteryWidgetReceiver"
    }
}

object BatteryMonitorServiceController {
    fun start(context: Context) {
        val intent = Intent(context, BatteryMonitorService::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("BatteryMonitorService", "Unable to start battery monitor service", e)
        }
    }

    fun stop(context: Context) {
        try {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("BatteryMonitorService", "Unable to stop battery monitor service", e)
        }
    }
}
