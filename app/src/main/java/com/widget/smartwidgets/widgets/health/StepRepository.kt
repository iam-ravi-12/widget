package com.widget.smartwidgets.widgets.health

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class StepRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASELINE_COUNT = "steps_baseline_count"
        private const val KEY_BASELINE_DATE = "steps_baseline_date"
        private const val KEY_LATEST_SENSOR_COUNT = "latest_sensor_count"
    }

    /**
     * Updates the latest sensor count and recalculates the daily baseline if necessary.
     * Returns the calculated steps for today.
     */
    fun updateStepsAndGetTodayCount(currentSensorCount: Float): Int {
        prefs.edit().putFloat(KEY_LATEST_SENSOR_COUNT, currentSensorCount).apply()

        val currentDate = LocalDate.now().toString()
        var baselineDate = prefs.getString(KEY_BASELINE_DATE, "") ?: ""
        var baselineCount = prefs.getFloat(KEY_BASELINE_COUNT, -1f)
        var reason = "existing_baseline"

        // Initialize or Midnight Reset
        if (baselineDate != currentDate) {
            reason = if (baselineDate.isEmpty()) "initialize" else "new_day_reset"
            baselineDate = currentDate
            baselineCount = currentSensorCount
            prefs.edit()
                .putString(KEY_BASELINE_DATE, baselineDate)
                .putFloat(KEY_BASELINE_COUNT, baselineCount)
                .apply()
        }

        // Reboot or Sensor Reset check
        if (baselineCount > currentSensorCount) {
            reason = "sensor_reset"
            baselineCount = currentSensorCount
            prefs.edit().putFloat(KEY_BASELINE_COUNT, baselineCount).apply()
        }

        val todaySteps = currentSensorCount - baselineCount
        val calculatedToday = todaySteps.toInt().coerceAtLeast(0)

        android.util.Log.d("STEPS_DEBUG", "STEPS_DEBUG\n" +
            "raw=${currentSensorCount.toInt()}\n" +
            "baseline=${baselineCount.toInt()}\n" +
            "baselineDate=$baselineDate\n" +
            "today=$currentDate\n" +
            "calculatedToday=$calculatedToday\n" +
            "reason=$reason\n" +
            "timestamp=${System.currentTimeMillis()}"
        )

        return calculatedToday
    }

    /**
     * Gets the current step count based on the last known sensor value.
     */
    fun getTodaySteps(): Int {
        val currentSensorCount = prefs.getFloat(KEY_LATEST_SENSOR_COUNT, -1f)
        if (currentSensorCount == -1f) return 0
        return updateStepsAndGetTodayCount(currentSensorCount)
    }
}
