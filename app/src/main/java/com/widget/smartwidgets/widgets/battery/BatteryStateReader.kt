package com.widget.smartwidgets.widgets.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Reads current battery state from the system's sticky broadcast.
 *
 * This is zero-cost: Android keeps the last ACTION_BATTERY_CHANGED intent
 * in memory as a sticky broadcast. Calling registerReceiver(null, filter)
 * returns it immediately without registering any ongoing listener.
 *
 * No background process, no polling, no service needed.
 */
object BatteryStateReader {

    fun read(context: Context): BatteryState {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        
        val intent: Intent? = try {
            context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        } catch (e: Exception) {
            android.util.Log.e("BatteryStateReader", "Failed to register battery receiver", e)
            null
        }

        var percentage = -1
        var isCharging = false
        var chargingType = BatteryState.ChargingType.NONE

        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            percentage = if (level >= 0 && scale > 0) (level * 100) / scale else -1

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            chargingType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> BatteryState.ChargingType.AC
                BatteryManager.BATTERY_PLUGGED_USB -> BatteryState.ChargingType.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryState.ChargingType.WIRELESS
                else -> BatteryState.ChargingType.NONE
            }
        } else {
            android.util.Log.w("BatteryStateReader", "ACTION_BATTERY_CHANGED intent is null")
        }

        // Fallback to BatteryManager system service if intent approach failed
        if (percentage < 0 && batteryManager != null) {
            percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                isCharging = batteryManager.isCharging
            }
        }

        return BatteryState(
            percentage = percentage,
            isCharging = isCharging,
            chargingType = chargingType
        )
    }
}
