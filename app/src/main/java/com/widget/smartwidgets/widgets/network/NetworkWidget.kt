package com.widget.smartwidgets.widgets.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.layout.fillMaxWidth
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard

/**
 * Network connectivity widget using Jetpack Glance.
 *
 * Reads current connectivity state from ConnectivityManager at render time.
 * Update triggers are managed by NetworkWidgetReceiver:
 * - Event-driven: CONNECTIVITY_CHANGE broadcasts
 * - Periodic fallback: 30-min WorkManager task
 */
class NetworkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val (icon, typeName) = when {
            !isConnected -> "🔴" to "Offline"
            isWifi -> "🟢" to "Wi-Fi"
            isCellular -> "🟢" to "Mobile Data"
            else -> "🟢" to "Connected"
        }

        provideContent {
            GlanceTheme {
                GlanceWidgetCard {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Network",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.clickable(actionRunCallback<RefreshNetworkAction>())
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = icon,
                            style = TextStyle(color = GlanceTheme.colors.onSurface)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = typeName,
                            style = TextStyle(color = GlanceTheme.colors.onSurface)
                        )
                    }
                }
            }
        }
    }
}

class RefreshNetworkAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NetworkWidget().update(context, glanceId)
    }
}
