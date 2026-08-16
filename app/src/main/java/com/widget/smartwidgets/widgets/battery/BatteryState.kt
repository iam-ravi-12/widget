package com.widget.smartwidgets.widgets.battery

/**
 * Battery state data class. Not persisted — read from system broadcasts at render time.
 */
data class BatteryState(
    val percentage: Int,
    val isCharging: Boolean,
    val chargingType: ChargingType
) {
    enum class ChargingType {
        NONE, USB, AC, WIRELESS
    }

    val statusText: String
        get() = when {
            isCharging -> when (chargingType) {
                ChargingType.USB -> "USB Charging"
                ChargingType.AC -> "Charging"
                ChargingType.WIRELESS -> "Wireless Charging"
                ChargingType.NONE -> "Charging"
            }
            else -> "Battery"
        }
}
