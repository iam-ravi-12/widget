package com.widget.smartwidgets.widgets.clock

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.GlanceId
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.widget.smartwidgets.R

class AnalogClockAWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockWidget(R.layout.widget_analog_clock_a)
}

class AnalogClockBWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockWidget(R.layout.widget_analog_clock_b)
}

class AnalogClockCWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockWidget(R.layout.widget_analog_clock_c)
}

class AnalogClockWidget(private val layoutResId: Int) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val remoteViews = RemoteViews(context.packageName, layoutResId)
            AndroidRemoteViews(remoteViews)
        }
    }
}
