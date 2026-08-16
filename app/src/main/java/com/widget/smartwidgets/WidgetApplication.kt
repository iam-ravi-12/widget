package com.widget.smartwidgets

import android.app.Application

class WidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Removed dynamic ACTION_BATTERY_CHANGED receiver to prevent process thrashing.
        // Removed global WidgetSyncWorker to prevent redundant updates.
        // Widgets will rely on Android's native updatePeriodMillis and explicit manifest receivers.
    }
}
