package com.widget.smartwidgets.widgets.battery

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard

class BatteryBarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val batteryState = BatteryStateReader.read(context)

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentPadding = 12.dp,
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Battery",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = "${batteryState.percentage}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        val totalBlocks = 20
                        val filledBlocks = (batteryState.percentage / 100f * totalBlocks).toInt()
                        val barText = "█".repeat(filledBlocks) + "░".repeat(totalBlocks - filledBlocks)
                        
                        Text(
                            text = barText,
                            style = TextStyle(
                                color = if (batteryState.percentage <= 20 && !batteryState.isCharging) 
                                    androidx.glance.color.ColorProvider(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Red) 
                                else GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        Text(
                            text = if (batteryState.isCharging) "Charging" else "Not charging",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
