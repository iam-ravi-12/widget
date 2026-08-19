package com.widget.smartwidgets.widgets.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaMonitorService : NotificationListenerService() {
    
    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers?.firstOrNull())
    }
    
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateWidgetData()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateWidgetData()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val componentName = ComponentName(this, MediaMonitorService::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            updateActiveController(controllers.firstOrNull())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeController?.unregisterCallback(callback)
    }

    private fun updateActiveController(controller: MediaController?) {
        if (activeController == controller) return
        
        activeController?.unregisterCallback(callback)
        activeController = controller
        activeController?.registerCallback(callback)
        
        updateWidgetData()
    }

    private fun updateWidgetData() {
        val controller = activeController
        if (controller == null) {
            MediaSessionData.hasActiveSession = false
            MediaSessionData.isPlaying = false
            MediaSessionData.title = null
            MediaSessionData.artist = null
            MediaSessionData.artwork = null
        } else {
            MediaSessionData.hasActiveSession = true
            MediaSessionData.isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            MediaSessionData.title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            MediaSessionData.artist = controller.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            MediaSessionData.artwork = controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MusicWidget().updateAll(applicationContext)
                MusicLargeWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    companion object {
        fun playPause(context: Context) {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                val controller = controllers.firstOrNull() ?: return
                
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls?.pause()
                } else {
                    controller.transportControls?.play()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        
        fun skipToNext(context: Context) {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                controllers.firstOrNull()?.transportControls?.skipToNext()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        
        fun skipToPrevious(context: Context) {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                controllers.firstOrNull()?.transportControls?.skipToPrevious()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
