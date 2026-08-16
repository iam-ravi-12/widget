package com.widget.smartwidgets.ui.widgetpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object WidgetPreviewTheme {
    val backgroundDay = Color(0xFFF5F5F7)
    val backgroundNight = Color(0xFF1A1A2E)
    val textPrimaryDay = Color(0xFF1A1A2E)
    val textPrimaryNight = Color(0xFFEAEAEA)
    val textSecondaryDay = Color(0xFF6B6B80)
    val textSecondaryNight = Color(0xFF8E8EA0)
    val accentDay = Color(0xFF7C3AED)
    val accentNight = Color(0xFF9F67FF)

    @Composable
    fun getBackgroundColor(isDark: Boolean = isSystemInDarkTheme()) =
        if (isDark) backgroundNight else backgroundDay

    @Composable
    fun getTextPrimaryColor(isDark: Boolean = isSystemInDarkTheme()) =
        if (isDark) textPrimaryNight else textPrimaryDay

    @Composable
    fun getTextSecondaryColor(isDark: Boolean = isSystemInDarkTheme()) =
        if (isDark) textSecondaryNight else textSecondaryDay

    @Composable
    fun getAccentColor(isDark: Boolean = isSystemInDarkTheme()) =
        if (isDark) accentNight else accentDay
}

@Composable
fun WidgetPreviewCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WidgetPreviewTheme.getBackgroundColor())
            .padding(16.dp)
    ) {
        content()
    }
}
