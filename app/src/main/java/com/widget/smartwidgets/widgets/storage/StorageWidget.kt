package com.widget.smartwidgets.widgets.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.Alignment
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard

/**
 * Storage usage widget using Jetpack Glance.
 *
 * Reads current storage usage from StatFs at render time.
 * Update triggers are managed by StorageWidgetReceiver:
 * - Event-driven: DEVICE_STORAGE_LOW / DEVICE_STORAGE_OK broadcasts
 * - Periodic fallback: 60-min WorkManager task
 */
class StorageWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - availableBytes

        val usedPercent = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes) * 100).toInt() else 0

        provideContent {
            GlanceTheme {
                GlanceWidgetCard {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Storage",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.clickable(actionRunCallback<RefreshStorageAction>())
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "${formatSize(usedBytes)} / ${formatSize(totalBytes)}",
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "$usedPercent% used",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
    }

    private fun formatSize(size: Long): String {
        var s = size.toDouble()
        val suffix = arrayOf("B", "KB", "MB", "GB", "TB")
        var index = 0
        while (s >= 1024 && index < suffix.size - 1) {
            s /= 1024
            index++
        }
        return String.format(java.util.Locale.US, "%.1f %s", s, suffix[index])
    }
}

class RefreshStorageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        StorageWidget().update(context, glanceId)
    }
}
