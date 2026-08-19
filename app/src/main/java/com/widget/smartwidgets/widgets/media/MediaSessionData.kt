package com.widget.smartwidgets.widgets.media

import android.graphics.Bitmap

object MediaSessionData {
    var isPlaying: Boolean = false
    var title: String? = null
    var artist: String? = null
    var artwork: Bitmap? = null
    
    var hasActiveSession: Boolean = false
}
