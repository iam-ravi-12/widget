package com.widget.smartwidgets.widgets.calendar

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Event-driven update mechanism for the Calendar widget.
 */
class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()

    private val scope = MainScope()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            Intent.ACTION_PROVIDER_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                scope.launch {
                    val manager = GlanceAppWidgetManager(context)
                    val ids = manager.getGlanceIds(CalendarWidget::class.java)
                    ids.forEach { id ->
                        glanceAppWidget.update(context, id)
                    }
                }
            }
        }
    }
}
