package com.widget.smartwidgets.widgets.calendar

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import com.widget.smartwidgets.widgets.common.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarDateMonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarDateMonthWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || 
            action == Intent.ACTION_TIMEZONE_CHANGED || 
            action == Intent.ACTION_TIME_CHANGED) {
            CoroutineScope(Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class CalendarDateMonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val monthStr = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek += 7
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysOfWeekList = listOf("M", "T", "W", "T", "F", "S", "S")

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentPadding = 8.dp,
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT SIDE: TODAY DATE
                        Column(
                            modifier = GlanceModifier.defaultWeight().padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = today.toString(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = monthStr,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = dayOfWeekStr,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        // RIGHT SIDE: MONTH CALENDAR
                        Column(
                            modifier = GlanceModifier.defaultWeight().defaultWeight()
                        ) {
                            Text(
                                text = monthYearFormat,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)
                            )
                            
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                for ((index, day) in daysOfWeekList.withIndex()) {
                                    Text(
                                        text = day,
                                        style = TextStyle(
                                            color = if (index == 6) androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.Red, night = androidx.compose.ui.graphics.Color.Red) else GlanceTheme.colors.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                }
                            }
                            
                            var currentDay = 1
                            var isFirstRow = true
                            
                            while (currentDay <= daysInMonth) {
                                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                    for (i in 0..6) {
                                        if (isFirstRow && i < firstDayOfWeek) {
                                            Box(modifier = GlanceModifier.defaultWeight()) {}
                                        } else if (currentDay <= daysInMonth) {
                                            val isToday = currentDay == today
                                            Box(
                                                modifier = GlanceModifier.defaultWeight(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = GlanceModifier
                                                        .size(18.dp)
                                                        .background(if (isToday) WidgetTheme.accent else androidx.glance.color.ColorProvider(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = currentDay.toString(),
                                                        style = TextStyle(
                                                            color = if (isToday) GlanceTheme.colors.surface else if (i == 6) androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.Red, night = androidx.compose.ui.graphics.Color.Red) else GlanceTheme.colors.onSurface,
                                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 10.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    )
                                                }
                                            }
                                            currentDay++
                                        } else {
                                            Box(modifier = GlanceModifier.defaultWeight()) {}
                                        }
                                    }
                                }
                                isFirstRow = false
                            }
                        }
                    }
                }
            }
        }
    }
}
