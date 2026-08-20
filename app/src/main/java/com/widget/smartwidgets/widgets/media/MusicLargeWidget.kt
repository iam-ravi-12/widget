package com.widget.smartwidgets.widgets.media

import android.content.Context
import android.media.session.PlaybackState
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard

class MusicLargeWidget : GlanceAppWidget() {
    
    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        provideContent {
            val trigger = MediaSessionData.updateTrigger
            GlanceTheme {
                val modifier = if (!hasPermission) {
                    GlanceModifier.fillMaxSize().clickable(actionRunCallback<MusicPermissionAction>())
                } else {
                    GlanceModifier.fillMaxSize()
                }

                GlanceWidgetCard(
                    modifier = modifier,
                    contentPadding = 16.dp,
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        if (hasPermission) {
                            if (MediaSessionData.hasActiveSession) {
                                Column(modifier = GlanceModifier.fillMaxSize()) {
                                    
                                    // Top row: Art + Metadata + Controls
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Circular Artwork
                                        if (MediaSessionData.artwork != null) {
                                            Image(
                                                provider = ImageProvider(MediaSessionData.artwork!!),
                                                contentDescription = "Album Art",
                                                modifier = GlanceModifier.size(72.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = GlanceModifier.size(72.dp).background(GlanceTheme.colors.surfaceVariant).cornerRadius(36.dp), 
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "🎵", style = TextStyle(fontSize = 32.sp))
                                            }
                                        }
                                        
                                        Spacer(modifier = GlanceModifier.width(16.dp))
                                        
                                        Column(modifier = GlanceModifier.defaultWeight()) {
                                            // Title
                                            Text(
                                                text = MediaSessionData.title ?: "Unknown",
                                                style = TextStyle(
                                                    color = GlanceTheme.colors.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                ),
                                                maxLines = 1
                                            )
                                            
                                            Spacer(modifier = GlanceModifier.height(4.dp))
                                            
                                            // Artist
                                            Text(
                                                text = MediaSessionData.artist ?: "Unknown Artist",
                                                style = TextStyle(
                                                    color = GlanceTheme.colors.onSurfaceVariant,
                                                    fontSize = 14.sp
                                                ),
                                                maxLines = 1
                                            )
                                            
                                            Spacer(modifier = GlanceModifier.height(12.dp))
                                            
                                            // Controls
                                            Row(
                                                modifier = GlanceModifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⏮",
                                                    style = TextStyle(fontSize = 24.sp, color = GlanceTheme.colors.onSurface),
                                                    modifier = GlanceModifier.clickable(actionRunCallback<MusicPrevAction>())
                                                )
                                                Spacer(modifier = GlanceModifier.width(20.dp))
                                                
                                                Box(
                                                    modifier = GlanceModifier
                                                        .size(44.dp)
                                                        .background(GlanceTheme.colors.surfaceVariant)
                                                        .cornerRadius(22.dp)
                                                        .clickable(actionRunCallback<MusicPlayPauseAction>()),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (MediaSessionData.playbackState == PlaybackState.STATE_PLAYING) "⏸" else "▶️",
                                                        style = TextStyle(fontSize = 20.sp, color = GlanceTheme.colors.onSurface)
                                                    )
                                                }
                                                
                                                Spacer(modifier = GlanceModifier.width(20.dp))
                                                Text(
                                                    text = "⏭",
                                                    style = TextStyle(fontSize = 24.sp, color = GlanceTheme.colors.onSurface),
                                                    modifier = GlanceModifier.clickable(actionRunCallback<MusicNextAction>())
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = GlanceModifier.height(12.dp))
                                    
                                    // Bottom row: Progress Bar
                                    val currentPos = MediaSessionData.currentPosition
                                    val duration = MediaSessionData.duration
                                    val progress = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                                    
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTime(currentPos),
                                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                        )
                                        
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = GlanceModifier.defaultWeight().height(4.dp),
                                            color = GlanceTheme.colors.primary,
                                            backgroundColor = GlanceTheme.colors.surfaceVariant
                                        )
                                        
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                        
                                        Text(
                                            text = formatTime(duration),
                                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "No media playing",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = GlanceModifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Permission required",
                                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp)
                                )
                                Spacer(modifier = GlanceModifier.height(8.dp))
                                Text(
                                    text = "Tap to enable",
                                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp)
                                )
                            }
                    }
                }
            }
        }
    }
}
