package com.widget.smartwidgets.widgets.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
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
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import com.widget.smartwidgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PomodoroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.widget.smartwidgets.POMODORO_SESSION_COMPLETE") {
            val appWidgetId = intent.getIntExtra("appWidgetId", -1)
            if (appWidgetId != -1) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleSessionComplete(context, appWidgetId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            PomodoroAlarmScheduler.cancelAlarm(context, appWidgetId)
            CoroutineScope(Dispatchers.IO).launch {
                prefs.setPreference(PreferencesKeys.pomodoroState(appWidgetId), "idle")
                prefs.setPreference(PreferencesKeys.pomodoroEndTime(appWidgetId), "0")
            }
        }
    }

    private suspend fun handleSessionComplete(context: Context, appWidgetId: Int) {
        val prefs = WidgetPreferences(context)
        val stateKey = PreferencesKeys.pomodoroState(appWidgetId)
        val endTimeKey = PreferencesKeys.pomodoroEndTime(appWidgetId)
        
        val currentState = prefs.getPreference(stateKey, "idle").firstOrNull() ?: "idle"
        
        if (currentState == "working") {
            prefs.setPreference(stateKey, "break")
            val nextEndTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 min break
            prefs.setPreference(endTimeKey, nextEndTime.toString())
            PomodoroAlarmScheduler.scheduleAlarm(context, appWidgetId, nextEndTime)
        } else if (currentState == "break") {
            prefs.setPreference(stateKey, "idle")
            prefs.setPreference(endTimeKey, "0")
            // No next alarm
        }
        
        val manager = GlanceAppWidgetManager(context)
        val glanceId = manager.getGlanceIdBy(appWidgetId)
        PomodoroWidget().update(context, glanceId)
    }
}

object PomodoroAlarmScheduler {
    fun scheduleAlarm(context: Context, appWidgetId: Int, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PomodoroWidgetReceiver::class.java).apply {
            action = "com.widget.smartwidgets.POMODORO_SESSION_COMPLETE"
            putExtra("appWidgetId", appWidgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                1000L * 60, // 1 minute window
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PomodoroWidgetReceiver::class.java).apply {
            action = "com.widget.smartwidgets.POMODORO_SESSION_COMPLETE"
            putExtra("appWidgetId", appWidgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class PomodoroWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val prefs = WidgetPreferences(context)
        val stateKey = PreferencesKeys.pomodoroState(appWidgetId)
        val endTimeKey = PreferencesKeys.pomodoroEndTime(appWidgetId)
        val remainingKey = androidx.datastore.preferences.core.longPreferencesKey("pomodoro_remaining_$appWidgetId")

        val state = prefs.getPreference(stateKey, "idle").firstOrNull() ?: "idle"
        val endTimeStr = prefs.getPreference(endTimeKey, "0").firstOrNull() ?: "0"
        val endTime = endTimeStr.toLongOrNull() ?: 0L
        val remainingAtPause = prefs.getPreference(remainingKey, 0L).firstOrNull() ?: 0L

        provideContent {
            val currentTime = System.currentTimeMillis()
            var remaining = if (state == "paused") remainingAtPause else endTime - currentTime
            
            if (state == "idle") {
                remaining = 25 * 60 * 1000L
            }
            if (remaining < 0) remaining = 0L

            val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
            val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            val timeString = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

            val displayState = state.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }

            GlanceTheme {
                GlanceWidgetCard(horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pomodoro",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontWeight = FontWeight.Medium),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.clickable(actionRunCallback<PomodoroRefreshAction>())
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (state == "working" || state == "break") {
                        val remoteViews = RemoteViews(context.packageName, R.layout.widget_pomodoro_timer)
                        val baseTime = SystemClock.elapsedRealtime() + (endTime - System.currentTimeMillis())
                        remoteViews.setLong(R.id.chronometer, "setBase", baseTime)
                        // Chronometer auto-starts when setBase is called if it's visible, but we can enforce it:
                        remoteViews.setBoolean(R.id.chronometer, "setStarted", true)
                        AndroidRemoteViews(remoteViews)
                    } else {
                        Text(
                            text = timeString,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = displayState,
                        style = TextStyle(color = GlanceTheme.colors.primary)
                    )
                    Spacer(modifier = GlanceModifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state == "idle" || remaining == 0L) {
                            Button(
                                text = "Start",
                                onClick = actionRunCallback<PomodoroActionCallback>(
                                    actionParametersOf(
                                        ActionParameters.Key<Int>("appWidgetId") to appWidgetId,
                                        ActionParameters.Key<String>("action") to "start"
                                    )
                                )
                            )
                        } else if (state == "working" || state == "break") {
                            Button(
                                text = "Pause",
                                onClick = actionRunCallback<PomodoroActionCallback>(
                                    actionParametersOf(
                                        ActionParameters.Key<Int>("appWidgetId") to appWidgetId,
                                        ActionParameters.Key<String>("action") to "pause"
                                    )
                                )
                            )
                        } else if (state == "paused") {
                            Button(
                                text = "Resume",
                                onClick = actionRunCallback<PomodoroActionCallback>(
                                    actionParametersOf(
                                        ActionParameters.Key<Int>("appWidgetId") to appWidgetId,
                                        ActionParameters.Key<String>("action") to "resume"
                                    )
                                )
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        
                        Button(
                            text = "Reset",
                            onClick = actionRunCallback<PomodoroActionCallback>(
                                actionParametersOf(
                                    ActionParameters.Key<Int>("appWidgetId") to appWidgetId,
                                    ActionParameters.Key<String>("action") to "reset"
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

class PomodoroRefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PomodoroWidget().update(context, glanceId)
    }
}

class PomodoroActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = parameters[ActionParameters.Key<Int>("appWidgetId")] ?: return
        val action = parameters[ActionParameters.Key<String>("action")] ?: return

        val prefs = WidgetPreferences(context)
        val stateKey = PreferencesKeys.pomodoroState(appWidgetId)
        val endTimeKey = PreferencesKeys.pomodoroEndTime(appWidgetId)
        val remainingKey = androidx.datastore.preferences.core.longPreferencesKey("pomodoro_remaining_$appWidgetId")

        val currentState = prefs.getPreference(stateKey, "idle").firstOrNull() ?: "idle"

        when (action) {
            "start" -> {
                val duration = 25 * 60 * 1000L
                val endTime = System.currentTimeMillis() + duration
                prefs.setPreference(stateKey, "working")
                prefs.setPreference(endTimeKey, endTime.toString())
                PomodoroAlarmScheduler.scheduleAlarm(context, appWidgetId, endTime)
            }
            "pause" -> {
                if (currentState == "working" || currentState == "break") {
                    val endTimeStr = prefs.getPreference(endTimeKey, "0").firstOrNull() ?: "0"
                    val endTime = endTimeStr.toLongOrNull() ?: 0L
                    val remaining = Math.max(0L, endTime - System.currentTimeMillis())
                    
                    prefs.setPreference(stateKey, "paused")
                    prefs.setPreference(remainingKey, remaining)
                    PomodoroAlarmScheduler.cancelAlarm(context, appWidgetId)
                }
            }
            "resume" -> {
                if (currentState == "paused") {
                    val remaining = prefs.getPreference(remainingKey, 0L).firstOrNull() ?: 0L
                    val newEndTime = System.currentTimeMillis() + remaining
                    prefs.setPreference(stateKey, "working")
                    prefs.setPreference(endTimeKey, newEndTime.toString())
                    PomodoroAlarmScheduler.scheduleAlarm(context, appWidgetId, newEndTime)
                }
            }
            "reset" -> {
                prefs.setPreference(stateKey, "idle")
                prefs.setPreference(endTimeKey, "0")
                PomodoroAlarmScheduler.cancelAlarm(context, appWidgetId)
            }
        }

        PomodoroWidget().update(context, glanceId)
    }
}
