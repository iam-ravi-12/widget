package com.widget.smartwidgets.widgets.battery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.updateAll
import com.widget.smartwidgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps a lightweight dynamic BATTERY_CHANGED receiver alive while at least one
 * Battery widget is placed. The receiver does not poll; Android pushes battery
 * changes to it and the widget is rendered from the latest BatteryStateReader value.
 */
class BatteryMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var receiverRegistered = false
    private var lastLevel = Int.MIN_VALUE
    private var lastStatus = Int.MIN_VALUE
    private var lastPlugged = Int.MIN_VALUE

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0)

            val changed = level != lastLevel || status != lastStatus || plugged != lastPlugged
            lastLevel = level
            lastStatus = status
            lastPlugged = plugged

            if (!changed) return

            serviceScope.launch {
                try {
                    BatteryWidget().updateAll(applicationContext)
                    BatteryBarWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to update battery widgets", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, filter)
        }
        receiverRegistered = true

        // Render the current value immediately when the service starts.
        serviceScope.launch {
            try {
                BatteryWidget().updateAll(applicationContext)
                BatteryBarWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Initial battery widget update failed", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val componentName1 = android.content.ComponentName(this, BatteryWidgetReceiver::class.java)
        val componentName2 = android.content.ComponentName(this, BatteryBarWidgetReceiver::class.java)
        val widgetIds1 = appWidgetManager.getAppWidgetIds(componentName1)
        val widgetIds2 = appWidgetManager.getAppWidgetIds(componentName2)

        if (widgetIds1.isEmpty() && widgetIds2.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered.
            }
            receiverRegistered = false
        }
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery widget active")
                .setContentText("Keeping battery widget data up to date")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Battery widget active")
                .setContentText("Keeping battery widget data up to date")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Battery widget monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps placed battery widgets updated when battery state changes."
                setShowBadge(false)
            }
        )
    }

    companion object {
        private const val TAG = "BatteryMonitorService"
        private const val CHANNEL_ID = "battery_widget_monitor"
        private const val NOTIFICATION_ID = 4201
        
        @Volatile
        var isRunning = false
    }
}
