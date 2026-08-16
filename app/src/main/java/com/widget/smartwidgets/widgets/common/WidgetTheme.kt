package com.widget.smartwidgets.widgets.common

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * Shared theme for all Glance widgets.
 * Provides day/night color pairs for consistent theming.
 */
object WidgetTheme {
    val background = ColorProvider(
        day = Color(0xFFF5F5F7),
        night = Color(0xFF1A1A2E)
    )
    val textPrimary = ColorProvider(
        day = Color(0xFF1A1A2E),
        night = Color(0xFFEAEAEA)
    )
    val textSecondary = ColorProvider(
        day = Color(0xFF6B6B80),
        night = Color(0xFF8E8EA0)
    )
    val accent = ColorProvider(
        day = Color(0xFF7C3AED),
        night = Color(0xFF9F67FF)
    )
}
