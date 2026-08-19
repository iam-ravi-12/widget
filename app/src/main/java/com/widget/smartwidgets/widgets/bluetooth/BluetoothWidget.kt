package com.widget.smartwidgets.widgets.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard

class BluetoothWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        
        var isConnected = false
        var deviceName: String? = null
        var hasPermission = true

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            val connectedDevices = bluetoothAdapter.bondedDevices.filter { device ->
                try {
                    val method = device.javaClass.getMethod("isConnected")
                    method.invoke(device) as Boolean
                } catch (e: Exception) {
                    false
                }
            }
            if (connectedDevices.isNotEmpty()) {
                isConnected = true
                deviceName = connectedDevices.first().name
            }
        }

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<BluetoothAction>()),
                    contentPadding = 12.dp,
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bluetooth",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        if (!hasPermission) {
                            Text(text = "Permission required", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(text = "Tap to enable", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
                        } else if (bluetoothAdapter?.isEnabled != true) {
                            Text(text = "Bluetooth is OFF", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
                        } else if (isConnected) {
                            Text(text = "🎧 $deviceName", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(text = "Connected", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
                        } else {
                            Text(text = "Not connected", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
                        }
                    }
                }
            }
        }
    }
}

class BluetoothAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        var hasPermission = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            val intent = Intent(context, BluetoothPermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
