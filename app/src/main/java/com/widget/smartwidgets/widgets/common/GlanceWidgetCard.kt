package com.widget.smartwidgets.widgets.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding

@Composable
fun GlanceWidgetCard(
    modifier: GlanceModifier = GlanceModifier,
    contentPadding: androidx.compose.ui.unit.Dp = 12.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        content = content
    )
}
