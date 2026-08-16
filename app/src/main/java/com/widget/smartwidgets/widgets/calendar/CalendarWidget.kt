package com.widget.smartwidgets.widgets.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.widgets.common.WidgetTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calendar widget using Jetpack Glance.
 */
class CalendarWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(120.dp, 120.dp)
        private val MEDIUM = DpSize(240.dp, 160.dp)
        private val LARGE = DpSize(280.dp, 280.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val events = if (hasPermission) {
            CalendarRepository.getUpcomingEvents(context)
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                CalendarContent(hasPermission, events)
            }
        }
    }
}

@Composable
private fun CalendarContent(hasPermission: Boolean, events: List<CalendarEvent>) {
    val size = LocalSize.current
    val isSmall = size.width < 180.dp || size.height < 150.dp
    val context = LocalContext.current

    val mainModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(16.dp)
        .background(WidgetTheme.background)
        .padding(if (isSmall) 12.dp else 16.dp)

    if (!hasPermission) {
        // No permission state
        Column(
            modifier = mainModifier.clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📅",
                style = TextStyle(fontSize = 32.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Enable Calendar Access",
                style = TextStyle(
                    color = WidgetTheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap to setup",
                style = TextStyle(
                    color = WidgetTheme.textSecondary,
                    fontSize = 12.sp
                )
            )
        }
        return
    }

    // Header date formatter
    val dateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    
    // Main content layout (launch calendar on tap)
    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("content://com.android.calendar/time/")
    }

    Column(
        modifier = mainModifier.clickable(actionStartActivity(calendarIntent))
    ) {
        // Header
        Text(
            text = dateString,
            style = TextStyle(
                color = WidgetTheme.accent,
                fontSize = if (isSmall) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = GlanceModifier.height(12.dp))

        if (events.isEmpty()) {
            Text(
                text = "No upcoming events",
                style = TextStyle(
                    color = WidgetTheme.textSecondary,
                    fontSize = 14.sp
                )
            )
        } else {
            val maxEvents = if (isSmall) 2 else if (size.height < 200.dp) 3 else 5
            
            events.take(maxEvents).forEach { event ->
                EventItem(event = event, isSmall = isSmall)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent, isSmall: Boolean) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = if (event.isAllDay) {
        "All day"
    } else {
        timeFormat.format(Date(event.startTime))
    }

    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = timeString,
            style = TextStyle(
                color = WidgetTheme.textSecondary,
                fontSize = if (isSmall) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.width(if (isSmall) 50.dp else 70.dp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = event.title,
            style = TextStyle(
                color = WidgetTheme.textPrimary,
                fontSize = if (isSmall) 12.sp else 14.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}
