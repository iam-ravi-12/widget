package com.widget.smartwidgets.widgets.status

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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

private const val WIDGET_PREFS = "widget_prefs"
private const val ACTION_CHECK_STOP = "CHECK_STOP"
private const val WIFI_AP_STATE_DISABLED = 11
private const val WIFI_AP_STATE_ENABLED = 13

private fun Context.startStatusMonitorService() {
    val intent = Intent(this, StatusWidgetsMonitorService::class.java)
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    } catch (e: Exception) {
        android.util.Log.e("StatusWidgets", "Unable to start status monitor service", e)
    }
}

private fun Context.checkStopStatusMonitorService() {
    val intent = Intent(this, StatusWidgetsMonitorService::class.java).apply {
        action = ACTION_CHECK_STOP
    }
    try {
        startService(intent)
    } catch (e: Exception) {
        android.util.Log.e("StatusWidgets", "Unable to request status monitor stop", e)
    }
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}



private fun CameraManager.findTorchCameraId(): String? {
    return cameraIdList.firstOrNull { id ->
        try {
            getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } catch (_: Exception) {
            false
        }
    }
}

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
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startStatusMonitorService()
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
    }
}

class AirplaneModeToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intentAirplane = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        if (intentAirplane.resolveActivity(context.packageManager) != null) {
            context.startActivity(intentAirplane)
        } else {
            val intentWireless = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            if (intentWireless.resolveActivity(context.packageManager) != null) {
                context.startActivity(intentWireless)
            } else {
                val intentSettings = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intentSettings)
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
                    onClickAction = actionRunCallback<AirplaneModeToggleAction>()
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
        context.startStatusMonitorService()
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
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
        context.startStatusMonitorService()
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
    }
}

class InternetToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class InternetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val hasInternet = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
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
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startStatusMonitorService()
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
    }
}

class LocationToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intentLocation = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        if (intentLocation.resolveActivity(context.packageManager) != null) {
            context.startActivity(intentLocation)
        } else {
            val intentSettings = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intentSettings)
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
                    onClickAction = actionRunCallback<LocationToggleAction>()
                )
            }
        }
    }
}

// 5. Do Not Disturb
class DoNotDisturbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DoNotDisturbWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startStatusMonitorService()
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
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
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startStatusMonitorService()
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
    }
}

class HotspotToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intentTether = Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        if (intentTether.resolveActivity(context.packageManager) != null) {
            context.startActivity(intentTether)
        } else {
            val intentWireless = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intentWireless)
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
                    statusText = "Tap to manage",
                    onClickAction = actionRunCallback<HotspotToggleAction>()
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
        context.startStatusMonitorService()
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startStatusMonitorService()
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.checkStopStatusMonitorService()
    }
}
class TorchToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            context.openAppSettings()
            return
        }

        val actualState = StatusWidgetsMonitorService.isTorchOn
        val newState = !actualState
        
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.findTorchCameraId() ?: return
            cm.setTorchMode(cameraId, newState)
        } catch (e: Exception) {
            android.util.Log.e("TorchWidget", "Unable to toggle torch", e)
        }
    }
}

class TorchWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasCameraPermission = context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val isTorchOn = StatusWidgetsMonitorService.isTorchOn
        
        provideContent {
            GlanceTheme {
                StatusWidgetLayout(
                    title = "Torch",
                    iconText = "🔦",
                    statusText = if (hasCameraPermission) {
                        if (isTorchOn) "ON" else "OFF"
                    } else {
                        "Perm Req"
                    },
                    onClickAction = actionRunCallback<TorchToggleAction>()
                )
            }
        }
    }
}
