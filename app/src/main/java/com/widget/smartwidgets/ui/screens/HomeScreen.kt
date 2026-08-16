package com.widget.smartwidgets.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.widget.smartwidgets.widgets.calendar.CalendarWidget
import kotlinx.coroutines.launch
import com.widget.smartwidgets.ui.widgetpreview.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNotes: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(CalendarWidget::class.java)
                val widget = CalendarWidget()
                ids.forEach { id ->
                    widget.update(context, id)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Widgets") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Battery-efficient home screen widgets", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("\uD83D\uDD0B Battery First", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All widgets are designed for minimal battery consumption. No background services, no polling, no unnecessary wake-ups.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CATEGORY: Productivity
            CategoryHeader("Productivity")
            WidgetCard("Quick Notes", "Quickly view and manage short notes.", "\uD83D\uDCDD", "Minimal") { NotesWidgetPreview() }
            WidgetCard("To-Do", "Interactive checklist.", "☑\uFE0F", "Minimal") { TodoWidgetPreview() }
            WidgetCard("Countdown", "Track upcoming events.", "⏳", "Minimal") { CountdownWidgetPreview() }
            WidgetCard("Pomodoro", "Focus timer.", "\uD83C\uDF45", "Minimal") { PomodoroWidgetPreview() }
            
            // CATEGORY: Time
            CategoryHeader("Time")
            WidgetCard("Clock", "Current time and date.", "\uD83D\uDD50", "None") { ClockWidgetPreview() }
            WidgetCard("World Clock", "Track time in multiple cities.", "\uD83C\uDF0F", "Minimal") { WorldClockWidgetPreview() }
            
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("\uD83D\uDCC5", fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp, top = 4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Displays upcoming events.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    CalendarWidgetPreview()
                    if (!hasCalendarPermission) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }, modifier = Modifier.fillMaxWidth()) { Text("Enable Calendar Access") }
                    }
                }
            }

            // CATEGORY: Information
            CategoryHeader("Information")
            WidgetCard("Weather", "Current weather.", "⛅", "Low") { WeatherWidgetPreview() }
            WidgetCard("Battery", "Battery percentage and state.", "\uD83D\uDD0B", "Minimal") { BatteryWidgetPreview() }
            WidgetCard("Storage", "Available device storage.", "\uD83D\uDCBE", "Minimal") { StorageWidgetPreview() }
            WidgetCard("Memory", "Available RAM.", "\uD83E\uDDE0", "Minimal") { MemoryWidgetPreview() }
            WidgetCard("Network", "Connection state.", "\uD83D\uDE36\u200D\uD83C\uDF2B\uFE0F", "Minimal") { NetworkWidgetPreview() }
            WidgetCard("Device Info", "Device hardware info.", "\uD83D\uDCF1", "Minimal") { DeviceInfoWidgetPreview() }

            // CATEGORY: Lifestyle
            CategoryHeader("Lifestyle")
            WidgetCard("Daily Quote", "Inspiring daily quotes.", "\uD83D\uDCD6", "None") { QuoteWidgetPreview() }
            WidgetCard("Photo Frame", "Display a favorite photo.", "\uD83D\uDDBC\uFE0F", "None") { PhotoFrameWidgetPreview() }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun WidgetCard(name: String, description: String, emoji: String, batteryImpact: String, previewContent: @Composable () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(emoji, fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp, top = 4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Battery impact: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(batteryImpact, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            previewContent()
        }
    }
}
