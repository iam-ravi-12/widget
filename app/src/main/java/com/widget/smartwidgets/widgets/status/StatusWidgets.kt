package com.widget.smartwidgets.widgets.status

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
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
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

@Composable
fun StatusWidgetLayout(
    title: String,
    iconText: String,
    statusText: String,
    onClickAction: Action? = null
) {
    var modifier = GlanceModifier.fillMaxSize()
    if (onClickAction != null) {
        modifier = modifier.clickable(onClickAction)
    }

    GlanceWidgetCard(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = iconText,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = statusText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// 1. Airplane Mode
class AirplaneModeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AirplaneModeWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}
class AirplaneModeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isAirplaneMode = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Airplane Mode",
                    iconText = "✈️",
                    statusText = if (isAirplaneMode) "ON" else "OFF",
                    onClickAction = actionStartActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                )
            }
        }
    }
}

// 2. Auto Rotation
class AutoRotationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AutoRotationWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java).apply {
            action = "CHECK_STOP"
        }
        context.startService(intent)
    }
}
class AutoRotationToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (Settings.System.canWrite(context)) {
            val isAutoRotation = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            ) == 1
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (isAutoRotation) 0 else 1
            )
            AutoRotationWidget().update(context, glanceId)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

class AutoRotationWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isAutoRotation = Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Auto Rotation",
                    iconText = "🔄",
                    statusText = if (isAutoRotation) "ON" else "OFF",
                    onClickAction = actionRunCallback<AutoRotationToggleAction>()
                )
            }
        }
    }
}

// 3. Internet
class InternetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = InternetWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java).apply {
            action = "CHECK_STOP"
        }
        context.startService(intent)
    }
}

class InternetToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val hasInternet = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val lastState = prefs.getBoolean("internet_last_state", false)
        
        if (hasInternet != lastState) {
            prefs.edit().putBoolean("internet_last_state", hasInternet).apply()
            InternetWidget().update(context, glanceId)
        } else {
            val intent = Intent(if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

class InternetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val hasInternet = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("internet_last_state", hasInternet).apply()
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Internet",
                    iconText = "🌐",
                    statusText = if (hasInternet) "ON" else "OFF",
                    onClickAction = actionRunCallback<InternetToggleAction>()
                )
            }
        }
    }
}

// 4. Location
class LocationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LocationWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}
class LocationWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationOn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Location",
                    iconText = "📍",
                    statusText = if (isLocationOn) "ON" else "OFF",
                    onClickAction = actionStartActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                )
            }
        }
    }
}

// 5. Do Not Disturb
class DoNotDisturbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DoNotDisturbWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}
class DndToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            val filter = nm.currentInterruptionFilter
            val isDndOn = filter == NotificationManager.INTERRUPTION_FILTER_NONE || filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY || filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
            
            val newFilter = if (isDndOn) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY
            nm.setInterruptionFilter(newFilter)
            
            // Re-read and update all immediately
            DoNotDisturbWidget().updateAll(context)
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

class DoNotDisturbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hasPermission = nm.isNotificationPolicyAccessGranted
        val statusText = if (hasPermission) {
            val filter = nm.currentInterruptionFilter
            if (filter == NotificationManager.INTERRUPTION_FILTER_NONE || filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY || filter == NotificationManager.INTERRUPTION_FILTER_ALARMS) "ON" else "OFF"
        } else {
            "Perm Req"
        }
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Do Not Disturb",
                    iconText = "🌙",
                    statusText = statusText,
                    onClickAction = actionRunCallback<DndToggleAction>()
                )
            }
        }
    }
}

// 6. Hotspot
class HotspotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HotspotWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class HotspotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Hotspot",
                    iconText = "📶",
                    statusText = "Unknown",
                    onClickAction = actionStartActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                )
            }
        }
    }
}

// 7. Torch
class TorchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TorchWidget()
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val intent = Intent(context, StatusWidgetsMonitorService::class.java).apply {
            action = "CHECK_STOP"
        }
        context.startService(intent)
    }
}
class TorchToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val isTorchOn = prefs.getBoolean("torch_state", false)
        val newState = !isTorchOn
        
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            cm.setTorchMode(cameraId, newState)
            // State is updated by the TorchMonitorService to ensure source of truth
        } catch (e: Exception) {
            // Ignore camera access issues
        }
    }
}

class TorchWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val isTorchOn = prefs.getBoolean("torch_state", false)
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Torch",
                    iconText = "🔦",
                    statusText = if (isTorchOn) "ON" else "OFF",
                    onClickAction = actionRunCallback<TorchToggleAction>()
                )
            }
        }
    }
}
