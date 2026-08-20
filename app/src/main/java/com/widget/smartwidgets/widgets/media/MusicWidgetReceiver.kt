package com.widget.smartwidgets.widgets.media

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import android.provider.Settings

import android.content.ComponentName
import android.service.notification.NotificationListenerService

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            NotificationListenerService.requestRebind(ComponentName(context, MediaMonitorService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class MusicLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicLargeWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            NotificationListenerService.requestRebind(ComponentName(context, MediaMonitorService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
