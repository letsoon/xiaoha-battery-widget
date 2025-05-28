package com.xiaoha.batterywidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BatteryWidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
} 