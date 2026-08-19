package com.widget.smartwidgets.widgets.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VolumeModeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VolumeModeWidget()

    private fun startMonitorService(context: Context) {
        val intent = Intent(context, com.widget.smartwidgets.widgets.status.StatusWidgetsMonitorService::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkStopMonitorService(context: Context) {
        val intent = Intent(context, com.widget.smartwidgets.widgets.status.StatusWidgetsMonitorService::class.java).apply {
            action = "CHECK_STOP"
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startMonitorService(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        startMonitorService(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        checkStopMonitorService(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    glanceAppWidget.updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
