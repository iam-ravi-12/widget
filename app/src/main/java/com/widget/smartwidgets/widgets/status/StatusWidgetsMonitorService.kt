package com.widget.smartwidgets.widgets.status

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusWidgetsMonitorService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var connectivityManager: ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var cameraId: String? = null

    // 1. Auto Rotation Observer
    private val rotationObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            scope.launch {
                try {
                    AutoRotationWidget().updateAll(this@StatusWidgetsMonitorService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 2. Internet Callback
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            scope.launch {
                try {
                    InternetWidget().updateAll(this@StatusWidgetsMonitorService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            scope.launch {
                try {
                    InternetWidget().updateAll(this@StatusWidgetsMonitorService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 3. Torch Callback
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            super.onTorchModeChanged(id, enabled)
            if (id == cameraId) {
                val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("torch_state", enabled).apply()
                scope.launch {
                    try {
                        TorchWidget().updateAll(this@StatusWidgetsMonitorService)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Auto Rotation
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            rotationObserver
        )

        // Internet
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Torch
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "CHECK_STOP") {
            if (shouldStopService()) {
                stopSelf()
                return START_NOT_STICKY
            }
            return START_STICKY
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "status_monitor_channel")
            .setContentTitle("Widget Sync Active")
            .setContentText("Listening for system changes to keep widgets accurate")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                2003,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(2003, notification)
        }
        
        return START_STICKY
    }

    private fun shouldStopService(): Boolean {
        val awm = AppWidgetManager.getInstance(this)
        val rotationIds = awm.getAppWidgetIds(ComponentName(this, AutoRotationWidgetReceiver::class.java))
        val internetIds = awm.getAppWidgetIds(ComponentName(this, InternetWidgetReceiver::class.java))
        val torchIds = awm.getAppWidgetIds(ComponentName(this, TorchWidgetReceiver::class.java))
        return rotationIds.isEmpty() && internetIds.isEmpty() && torchIds.isEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(rotationObserver)
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "status_monitor_channel",
                "System Widget Sync",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "Keeps dynamic widgets synchronized with actual device state"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
