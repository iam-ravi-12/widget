package com.widget.smartwidgets.widgets.photoframe

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.background
import androidx.glance.color.ColorProvider
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.core.datastore.PreferencesKeys
import com.widget.smartwidgets.core.datastore.WidgetPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import androidx.compose.ui.graphics.Color

class PhotoFrameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhotoFrameWidget()
}

class PhotoFrameWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val prefs = WidgetPreferences(context)
        val uriKey = PreferencesKeys.photoFrameUri(appWidgetId)
        val photoUri = prefs.getPreference(uriKey, "").firstOrNull()

        provideContent {
            val configIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("smartwidgets://photoframe/config/$appWidgetId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(day = Color.DarkGray, night = Color.DarkGray))
                    .clickable(actionStartActivity(configIntent)),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrBlank()) {
                    val file = File(context.filesDir, photoUri)
                    if (file.exists()) {
                        // Create bitmap or use file URI provider
                        // Glance supports bitamp or resource, but we can't directly load arbitrary files easily
                        // Note: For simplicity and reliability in Glance, loading it as a bitmap might OOM if too big.
                        // But since we copy it locally, we should use a content URI if possible.
                        // Actually, Glance provides ImageProvider(bitmap) or ImageProvider(uri).
                        Image(
                            provider = ImageProvider(android.graphics.BitmapFactory.decodeFile(file.absolutePath)),
                            contentDescription = "Photo Frame",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Photo missing",
                            style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White))
                        )
                    }
                } else {
                    Text(
                        text = "Tap to setup photo",
                        style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
