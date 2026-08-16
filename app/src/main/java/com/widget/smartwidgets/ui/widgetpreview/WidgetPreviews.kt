package com.widget.smartwidgets.ui.widgetpreview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClockWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("12:45 PM", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(4.dp))
            Text("Friday, Aug 15", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun BatteryWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("72%", fontSize = 30.sp, fontWeight = FontWeight.Medium, color = WidgetPreviewTheme.getTextPrimaryColor())
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("Charging", fontSize = 13.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun CalendarWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Today", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("10:00 AM - Team Meeting", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Text("1:00 PM - Lunch", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun WeatherWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Patna", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("31°C", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
            }
            Text("Sunny", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun NotesWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Quick Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Complete project", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Text("• Study Kotlin", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun TodoWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("To-Do", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("☑\uFE0F Buy groceries", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Text("☐ Call mom", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun CountdownWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Vacation", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Spacer(modifier = Modifier.height(4.dp))
            Text("14 Days", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
        }
    }
}

@Composable
fun PomodoroWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Focus Time", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Spacer(modifier = Modifier.height(4.dp))
            Text("25:00", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
        }
    }
}

@Composable
fun WorldClockWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("New York", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
                Text("03:15 AM", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
            }
            Column {
                Text("London", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
                Text("08:15 AM", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WidgetPreviewTheme.getTextPrimaryColor())
            }
        }
    }
}

@Composable
fun QuoteWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("\"The only way to do great work is to love what you do.\"", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("- Steve Jobs", fontSize = 12.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun PhotoFrameWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("🖼\uFE0F Photo", fontSize = 24.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun StorageWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Storage", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("45 GB / 128 GB Free", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun MemoryWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Memory", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("2.4 GB / 8.0 GB Available", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun NetworkWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Network", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Wi-Fi Connected", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Text("Home_Network_5G", fontSize = 12.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}

@Composable
fun DeviceInfoWidgetPreview() {
    WidgetPreviewCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Device", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetPreviewTheme.getTextPrimaryColor())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pixel 6", fontSize = 14.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
            Text("Android 14", fontSize = 12.sp, color = WidgetPreviewTheme.getTextSecondaryColor())
        }
    }
}
