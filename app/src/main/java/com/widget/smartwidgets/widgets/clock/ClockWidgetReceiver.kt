package com.widget.smartwidgets.widgets.clock

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Actual provider for the live Clock widget.
 * The UI is rendered by ClockWidget using native TextClock views.
 */
class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}
