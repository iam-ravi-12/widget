package com.widget.smartwidgets.widgets.clock

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.widget.smartwidgets.R

/**
 * Battery-efficient clock widget using Jetpack Glance.
 *
 * Architecture decision: Uses Android's built-in TextClock via AndroidRemoteViews
 * for real-time clock rendering. TextClock is rendered natively by the system UI
 * with zero app code running — the most battery-efficient approach possible.
 *
 * No background service, no polling, no WorkManager needed for clock updates.
 */
class ClockWidget : GlanceAppWidget() {

    // Exact mode: widget renders at whatever size the launcher gives it
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ClockContent()
        }
    }
}

@androidx.compose.runtime.Composable
private fun ClockContent() {
    val context = LocalContext.current
    val clockViews = RemoteViews(context.packageName, R.layout.widget_clock_layout)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidRemoteViews(clockViews)
    }
}
