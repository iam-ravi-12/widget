package com.widget.smartwidgets.widgets.calendar

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import com.widget.smartwidgets.widgets.common.WidgetTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthCalendarWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || 
            action == Intent.ACTION_TIMEZONE_CHANGED || 
            action == Intent.ACTION_TIME_CHANGED) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class MonthCalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek += 7
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    contentPadding = 4.dp,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = monthYearFormat,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.padding(vertical = 2.dp)
                    )
                    
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        for ((index, day) in daysOfWeek.withIndex()) {
                            Text(
                                text = day,
                                style = TextStyle(
                                    color = if (index == 6) androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.Red, night = androidx.compose.ui.graphics.Color.Red) else GlanceTheme.colors.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 9.sp,
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
                                                .size(16.dp)
                                                .background(if (isToday) WidgetTheme.accent else androidx.glance.color.ColorProvider(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentDay.toString(),
                                                style = TextStyle(
                                                    color = if (isToday) GlanceTheme.colors.surface else if (i == 6) androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.Red, night = androidx.compose.ui.graphics.Color.Red) else GlanceTheme.colors.onSurface,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 9.sp,
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
