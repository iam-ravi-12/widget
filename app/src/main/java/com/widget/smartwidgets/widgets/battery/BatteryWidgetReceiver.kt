package com.widget.smartwidgets.widgets.battery

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for the Battery widget.
 */
class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_POWER_CONNECTED ||
            action == Intent.ACTION_POWER_DISCONNECTED
        ) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    BatteryWidget().updateAll(context)
                } catch (e: Exception) {
                    android.util.Log.e("BatteryWidgetReceiver", "Failed to update battery widgets", e)
                }
            }
        }
    }
}
