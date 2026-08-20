package com.widget.smartwidgets.ui.screens.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.widget.smartwidgets.utils.UsageAccessUtil
import com.widget.smartwidgets.widgets.bluetooth.BluetoothWidget
import com.widget.smartwidgets.widgets.calendar.CalendarWidget
import com.widget.smartwidgets.widgets.health.StepsWidget
import com.widget.smartwidgets.widgets.media.MediaMonitorService
import com.widget.smartwidgets.widgets.media.MusicLargeWidget
import com.widget.smartwidgets.widgets.media.MusicWidget
import com.widget.smartwidgets.widgets.status.LocationWidget
import com.widget.smartwidgets.widgets.status.TorchWidget
import kotlinx.coroutines.launch

data class AppPermissionState(
    val notificationListenerGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val activityRecognitionGranted: Boolean = false,
    val bluetoothConnectGranted: Boolean = false,
    val modifySettingsGranted: Boolean = false,
    val dndAccessGranted: Boolean = false,
    val calendarGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val locationServicesEnabled: Boolean = false,
    val cameraGranted: Boolean = false
) {
    val totalCount: Int = 9
    val grantedCount: Int
        get() {
            var count = 0
            if (notificationListenerGranted) count++
            if (usageAccessGranted) count++
            if (activityRecognitionGranted) count++
            if (bluetoothConnectGranted) count++
            if (modifySettingsGranted) count++
            if (dndAccessGranted) count++
            if (calendarGranted) count++
            if (locationGranted) count++
            if (cameraGranted) count++
            return count
        }
}

fun checkAllPermissions(context: Context): AppPermissionState {
    // 1. Notification Listener
    val notificationListenerGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

    // 2. Usage Access
    val usageAccessGranted = UsageAccessUtil.hasUsageStatsPermission(context)

    // 3. Activity Recognition
    val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    // 4. Bluetooth Connect
    val bluetoothConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    // 5. Modify System Settings
    val modifySettingsGranted = Settings.System.canWrite(context)

    // 6. Do Not Disturb Access
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val dndAccessGranted = notificationManager.isNotificationPolicyAccessGranted

    // 7. Calendar
    val calendarGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    // 8. Location (Permission + Service Switch)
    val locationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val locationServicesEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // 9. Camera
    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    return AppPermissionState(
        notificationListenerGranted = notificationListenerGranted,
        usageAccessGranted = usageAccessGranted,
        activityRecognitionGranted = activityRecognitionGranted,
        bluetoothConnectGranted = bluetoothConnectGranted,
        modifySettingsGranted = modifySettingsGranted,
        dndAccessGranted = dndAccessGranted,
        calendarGranted = calendarGranted,
        locationGranted = locationGranted,
        locationServicesEnabled = locationServicesEnabled,
        cameraGranted = cameraGranted
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf(checkAllPermissions(context)) }

    fun refreshState() {
        val newState = checkAllPermissions(context)
        state = newState

        // If notification listener is granted, request rebind & update media widgets
        if (newState.notificationListenerGranted) {
            try {
                NotificationListenerService.requestRebind(ComponentName(context, MediaMonitorService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            coroutineScope.launch {
                try {
                    MusicWidget().updateAll(context)
                    MusicLargeWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // ON_RESUME lifecycle observer to automatically re-check permissions when returning to the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Activity Result Launchers for Runtime Permissions
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        refreshState()
        if (isGranted) {
            val intent = Intent(context, com.widget.smartwidgets.widgets.health.StepMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            coroutineScope.launch {
                try {
                    StepsWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
        coroutineScope.launch {
            try {
                BluetoothWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
        coroutineScope.launch {
            try {
                CalendarWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
        coroutineScope.launch {
            try {
                LocationWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
        coroutineScope.launch {
            try {
                TorchWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Summary Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.grantedCount == state.totalCount)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Permission Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${state.grantedCount} / ${state.totalCount} Granted",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { state.grantedCount.toFloat() / state.totalCount.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.grantedCount == state.totalCount)
                            "All permissions granted! All smart widgets have full functionality."
                        else
                            "Grant missing permissions below so their corresponding widgets can function properly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "REQUIRED PERMISSIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Notification Listener Access
            PermissionItemCard(
                icon = "🎵",
                name = "Notification Listener",
                description = "Required for Music & Music Wide widgets to read active track and playback state.",
                isGranted = state.notificationListenerGranted,
                actionLabel = "Enable",
                onAction = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallbackIntent)
                    }
                }
            )

            // 2. Usage Access
            PermissionItemCard(
                icon = "📊",
                name = "Usage Access",
                description = "Required for Screen Time & App Usage widgets to display daily app screen time.",
                isGranted = state.usageAccessGranted,
                actionLabel = "Enable",
                onAction = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallbackIntent)
                    }
                }
            )

            // 3. Activity Recognition
            PermissionItemCard(
                icon = "👟",
                name = "Activity Recognition",
                description = "Required for the Steps widget to count and track daily step progress.",
                isGranted = state.activityRecognitionGranted,
                actionLabel = "Enable",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
            )

            // 4. Bluetooth Connect
            PermissionItemCard(
                icon = "📶",
                name = "Bluetooth Connect",
                description = "Required for the Bluetooth widget to read connection state and device names.",
                isGranted = state.bluetoothConnectGranted,
                actionLabel = "Enable",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
            )

            // 5. Modify System Settings
            PermissionItemCard(
                icon = "🔄",
                name = "Modify System Settings",
                description = "Required for the Auto Rotation widget to toggle system display orientation lock.",
                isGranted = state.modifySettingsGranted,
                actionLabel = "Enable",
                onAction = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    }
                }
            )

            // 6. Do Not Disturb Access
            PermissionItemCard(
                icon = "🌙",
                name = "Do Not Disturb Access",
                description = "Required for the Do Not Disturb widget to switch system interruption modes.",
                isGranted = state.dndAccessGranted,
                actionLabel = "Enable",
                onAction = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallbackIntent)
                    }
                }
            )

            // 7. Calendar
            PermissionItemCard(
                icon = "📅",
                name = "Calendar",
                description = "Required for Calendar, Today Date, and Month widgets to show upcoming events.",
                isGranted = state.calendarGranted,
                actionLabel = "Enable",
                onAction = {
                    calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            )

            // 8. Location
            PermissionItemCard(
                icon = "📍",
                name = "Location Permission",
                description = "Required for Weather widget to retrieve local weather data automatically.",
                isGranted = state.locationGranted,
                actionLabel = "Enable",
                onAction = {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                extraContent = {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Location Services: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (state.locationServicesEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (state.locationServicesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!state.locationServicesEnabled) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Turn On", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            )

            // 9. Camera
            PermissionItemCard(
                icon = "📷",
                name = "Camera",
                description = "Required for the Torch widget to operate flashlight hardware.",
                isGranted = state.cameraGranted,
                actionLabel = "Enable",
                onAction = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: String,
    name: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                StatusBadge(isGranted = isGranted)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            extraContent?.invoke()

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isGranted: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isGranted)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isGranted) "Granted" else "Not granted",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
