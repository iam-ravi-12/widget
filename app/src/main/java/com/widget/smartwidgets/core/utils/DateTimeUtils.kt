package com.widget.smartwidgets.core.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date/time formatting utilities for widget display.
 * Uses java.time APIs (available on API 26+, our minSdk).
 */
object DateTimeUtils {

    fun currentTime(use24Hour: Boolean = true): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        )
    }

    fun currentDate(): String {
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        )
    }

    fun currentShortDate(): String {
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
        )
    }
}
