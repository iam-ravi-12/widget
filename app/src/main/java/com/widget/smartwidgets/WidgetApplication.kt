package com.widget.smartwidgets

import android.app.Application

class WidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dynamic widget-specific background components are started by each widget's
        // lifecycle. BatteryMonitorService is started while at least one Battery
        // widget exists, so no global worker is kept alive for unrelated widgets.
    }
}
