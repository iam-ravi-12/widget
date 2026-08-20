package com.widget.smartwidgets.widgets.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaMonitorService : NotificationListenerService() {
    
    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var progressJob: Job? = null
    
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(resolveCurrentMediaController(controllers))
    }
    
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateWidgetData()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateWidgetData()
        }
        
        override fun onSessionDestroyed() {
            // If the current session is destroyed, re-resolve the active session list
            try {
                val componentName = ComponentName(this@MediaMonitorService, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                updateActiveController(resolveCurrentMediaController(controllers))
            } catch (e: Exception) {
                updateActiveController(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val componentName = ComponentName(this, MediaMonitorService::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            updateActiveController(resolveCurrentMediaController(controllers))
        } catch (e: SecurityException) {
            android.util.Log.e("MediaMonitorService", "SecurityException in onListenerConnected", e)
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
        stopProgressUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        activeController?.unregisterCallback(callback)
        stopProgressUpdate()
        serviceScope.cancel()
    }

    private fun updateActiveController(controller: MediaController?) {
        val sameSession = activeController?.sessionToken == controller?.sessionToken && (activeController == null) == (controller == null)
        if (sameSession) {
            updateWidgetData()
            return
        }
        
        activeController?.unregisterCallback(callback)
        activeController = controller
        activeController?.registerCallback(callback)
        
        updateWidgetData()
    }

    private fun updateWidgetData(currentController: MediaController? = activeController) {
        try {
            val controller = currentController
            if (controller == null) {
                MediaSessionData.hasActiveSession = false
                MediaSessionData.isPlaying = false
                MediaSessionData.title = null
                MediaSessionData.artist = null
                MediaSessionData.artwork = null
                MediaSessionData.mediaId = null
                MediaSessionData.duration = 0L
                MediaSessionData.position = 0L
                MediaSessionData.currentPosition = 0L
                MediaSessionData.lastPositionUpdateTime = 0L
                MediaSessionData.playbackSpeed = 1f
                MediaSessionData.playbackState = 0
                
                stopProgressUpdate()
            } else {
                MediaSessionData.hasActiveSession = true
                
                val state = controller.playbackState
                val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                MediaSessionData.isPlaying = isPlaying
                MediaSessionData.playbackState = state?.state ?: 0
                MediaSessionData.lastPositionUpdateTime = state?.lastPositionUpdateTime ?: 0L
                MediaSessionData.playbackSpeed = state?.playbackSpeed ?: 1f
                
                val metadata = controller.metadata
                val fallbackId = metadata?.let {
                    val title = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                    val artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                    val album = it.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                    if (title.isNotEmpty() || artist.isNotEmpty()) "$title|$artist|$album" else null
                }
                val newMediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: fallbackId
                
                val songChanged = MediaSessionData.mediaId != newMediaId
                if (songChanged) {
                    MediaSessionData.mediaId = newMediaId
                    MediaSessionData.artwork = null
                }
                
                MediaSessionData.title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                MediaSessionData.artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                MediaSessionData.duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                
                val rawPos = state?.position ?: 0L
                if (songChanged) {
                    MediaSessionData.position = 0L
                } else {
                    MediaSessionData.position = rawPos
                }
                
                if (MediaSessionData.playbackState == PlaybackState.STATE_PLAYING && MediaSessionData.lastPositionUpdateTime > 0L) {
                    val lastUpdate = MediaSessionData.lastPositionUpdateTime
                    val elapsed = if (lastUpdate > 1000000000000L) {
                        System.currentTimeMillis() - lastUpdate
                    } else {
                        android.os.SystemClock.elapsedRealtime() - lastUpdate
                    }
                    val validElapsed = if (elapsed >= 0) elapsed else 0L
                    
                    val estimated = MediaSessionData.position + (validElapsed * MediaSessionData.playbackSpeed).toLong()
                    MediaSessionData.currentPosition = if (MediaSessionData.duration > 0) {
                        estimated.coerceIn(0L, MediaSessionData.duration)
                    } else {
                        estimated.coerceAtLeast(0L)
                    }
                } else {
                    MediaSessionData.currentPosition = MediaSessionData.position
                }
                
                if (MediaSessionData.artwork == null) {
                    val rawArtwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    MediaSessionData.artwork = rawArtwork?.let { getCircularBitmap(it) }
                }
                
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
            
            updateWidgets()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        if (size <= 0) return bitmap
        
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        
        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
        
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0x1000000
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squaredBitmap, rect, rect, paint)
        
        if (squaredBitmap != bitmap) {
            squaredBitmap.recycle()
        }
        return output
    }
    
    private fun refreshCurrentState() {
        try {
            val componentName = ComponentName(this, MediaMonitorService::class.java)
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            val controller = resolveCurrentMediaController(controllers)
            if (activeController?.sessionToken != controller?.sessionToken) {
                updateActiveController(controller)
            } else {
                updateWidgetData(controller)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startProgressUpdate() {
        if (progressJob?.isActive == true) return
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL_MS)
                refreshCurrentState()
            }
        }
    }
    
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateWidgets() {
        MediaSessionData.updateTrigger++
        serviceScope.launch {
            try {
                MusicWidget().updateAll(applicationContext)
                MusicLargeWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    companion object {
        var instance: MediaMonitorService? = null
        private const val PROGRESS_UPDATE_INTERVAL_MS = 1000L

        fun resolveCurrentMediaController(controllers: List<MediaController>?): MediaController? {
            if (controllers.isNullOrEmpty()) return null
            var bestController: MediaController? = null
            var bestPriority = Int.MAX_VALUE
            var bestUpdateTime = -1L
            for (controller in controllers) {
                val state = controller.playbackState?.state
                val metadata = controller.metadata
                val hasMetadata = metadata != null
                
                val priority = when {
                    state == PlaybackState.STATE_PLAYING && hasMetadata -> 1
                    state == PlaybackState.STATE_BUFFERING && hasMetadata -> 2
                    state == PlaybackState.STATE_PAUSED && hasMetadata -> 3
                    hasMetadata -> 4
                    else -> 5
                }
                
                if (priority > 4) continue
                
                val updateTime = controller.playbackState?.lastPositionUpdateTime ?: 0L
                
                if (priority < bestPriority || (priority == bestPriority && updateTime > bestUpdateTime)) {
                    bestPriority = priority
                    bestUpdateTime = updateTime
                    bestController = controller
                }
            }
            return bestController
        }
        
        fun playPause(context: Context) {
            try {
                val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                val controller = resolveCurrentMediaController(controllers) ?: return
                
                val currentState = controller.playbackState?.state
                when (currentState) {
                    PlaybackState.STATE_PLAYING -> controller.transportControls?.pause()
                    PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> controller.transportControls?.play()
                    else -> controller.transportControls?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        fun skipToNext(context: Context) {
            try {
                val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                val controller = resolveCurrentMediaController(controllers)
                controller?.transportControls?.skipToNext()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        fun skipToPrevious(context: Context) {
            try {
                val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName = ComponentName(context, MediaMonitorService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                val controller = resolveCurrentMediaController(controllers)
                controller?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
