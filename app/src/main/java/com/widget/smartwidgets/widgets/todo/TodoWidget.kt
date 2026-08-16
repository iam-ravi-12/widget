package com.widget.smartwidgets.widgets.todo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextDecoration
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.core.database.AppDatabase
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        fun updateAllInstances(context: Context) {
            GlobalScope.launch {
                val manager = GlanceAppWidgetManager(context)
                val widget = TodoWidget()
                val glanceIds = manager.getGlanceIds(TodoWidget::class.java)
                glanceIds.forEach { glanceId ->
                    widget.update(context, glanceId)
                }
            }
        }
    }
}

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val db = AppDatabase.getInstance(context)
        val topTasks = db.todoDao().getTopTasksForWidget()

        provideContent {
            val configIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartwidgets://todo")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            GlanceTheme {
                GlanceWidgetCard {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(configIntent)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To-Do",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "+",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (topTasks.isEmpty()) {
                        Text(
                            text = "No pending tasks",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        topTasks.forEach { task ->
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = if (task.isCompleted) "☑" else "☐"
                                val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                val color = if (task.isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface

                                Text(
                                    text = icon,
                                    style = TextStyle(color = color, fontSize = 18.sp),
                                    modifier = GlanceModifier.clickable(
                                        actionRunCallback<ToggleTaskAction>(
                                            actionParametersOf(
                                                ActionParameters.Key<Long>("taskId") to task.id,
                                                ActionParameters.Key<Boolean>("newState") to !task.isCompleted
                                            )
                                        )
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = task.title,
                                    style = TextStyle(color = color, textDecoration = textDecoration),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionParameters.Key<Long>("taskId")] ?: return
        val newState = parameters[ActionParameters.Key<Boolean>("newState")] ?: return

        val db = AppDatabase.getInstance(context)
        db.todoDao().updateTaskStatus(taskId, newState)

        TodoWidgetReceiver.updateAllInstances(context)
    }
}
