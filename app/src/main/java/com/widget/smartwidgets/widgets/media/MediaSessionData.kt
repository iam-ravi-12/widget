package com.widget.smartwidgets.widgets.media

import android.graphics.Bitmap
import android.os.SystemClock
import android.media.session.PlaybackState

object MediaSessionData {
    var updateTrigger: Int = 0
    
    var isPlaying: Boolean = false
    var title: String? = null
    var artist: String? = null
    var artwork: Bitmap? = null
    var mediaId: String? = null
    
    var hasActiveSession: Boolean = false
    
    var duration: Long = 0L
    var position: Long = 0L
    var lastPositionUpdateTime: Long = 0L
    var playbackSpeed: Float = 1f
    var playbackState: Int = 0
    
    var currentPosition: Long = 0L
}
