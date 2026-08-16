package com.widget.smartwidgets.widgets.battery

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.WidgetTheme

/**
 * Battery-efficient battery status widget using Jetpack Glance.
 *
 * Update mechanism:
 * - BatteryWidgetReceiver listens for ACTION_POWER_CONNECTED / DISCONNECTED (manifest-registered)
 * - On each update, reads battery state from the sticky ACTION_BATTERY_CHANGED broadcast
 * - Renders the UI, then the process becomes idle — no persistent background work
 *
 * The sticky broadcast read is zero-cost: Android keeps the last battery intent in memory.
 */
class BatteryWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(120.dp, 60.dp)
        private val MEDIUM = DpSize(200.dp, 80.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val batteryState = BatteryStateReader.read(context)

        provideContent {
            GlanceTheme {
                BatteryContent(batteryState)
            }
        }
    }
}

@Composable
private fun BatteryContent(state: BatteryState) {
    val size = LocalSize.current
    val isSmall = size.width < 180.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(WidgetTheme.background)
            .padding(if (isSmall) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (isSmall) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // Percentage row
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (state.isCharging) "⚡" else "🔋"
            Text(
                text = icon,
                style = TextStyle(
                    fontSize = if (isSmall) 18.sp else 22.sp,
                ),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = if (state.percentage >= 0) "${state.percentage}%" else "--%",
                style = TextStyle(
                    color = WidgetTheme.textPrimary,
                    fontSize = if (isSmall) 22.sp else 30.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // Manual Refresh Button
            Text(
                text = "↻",
                style = TextStyle(
                    color = WidgetTheme.textSecondary,
                    fontSize = 16.sp
                ),
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<RefreshBatteryAction>())
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        // Status text
        Text(
            text = state.statusText,
            style = TextStyle(
                color = WidgetTheme.textSecondary,
                fontSize = if (isSmall) 11.sp else 13.sp,
            ),
        )
    }
}

class RefreshBatteryAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        BatteryWidget().update(context, glanceId)
    }
}
