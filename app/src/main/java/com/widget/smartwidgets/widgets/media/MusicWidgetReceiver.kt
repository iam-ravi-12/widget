package com.widget.smartwidgets.widgets.media

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import android.provider.Settings

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}

class MusicLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicLargeWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}
