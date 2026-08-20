package com.widget.smartwidgets.widgets.media

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaPermissionActivity : ComponentActivity() {

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateWidgetsAndFinish()
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (hasPermission) {
            updateWidgetsAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (!hasPermission) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                settingsLauncher.launch(intent)
            } else {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                settingsLauncher.launch(fallbackIntent)
            }
        } else {
            updateWidgetsAndFinish()
        }
    }

    private fun updateWidgetsAndFinish() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (hasPermission) {
            try {
                android.service.notification.NotificationListenerService.requestRebind(
                    android.content.ComponentName(this, MediaMonitorService::class.java)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MusicWidget().updateAll(applicationContext)
                MusicLargeWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        finish()
    }
}
