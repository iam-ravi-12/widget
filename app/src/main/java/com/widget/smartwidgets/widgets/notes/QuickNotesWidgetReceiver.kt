package com.widget.smartwidgets.widgets.notes

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class QuickNotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QuickNotesWidget()

    companion object {
        private val scope = MainScope()

        /**
         * Called by the application (e.g., NoteRepository) to immediately update
         * all Quick Notes widgets when the underlying database changes.
         * This avoids polling or WorkManager and makes it entirely event-driven.
         */
        fun updateAllInstances(context: Context) {
            scope.launch {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(QuickNotesWidget::class.java)
                val widget = QuickNotesWidget()
                ids.forEach { id ->
                    widget.update(context, id)
                }
            }
        }
    }
}
