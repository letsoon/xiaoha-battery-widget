package com.xiaoha.batterywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var widgetCountText: TextView
    private lateinit var addWidgetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        startActivity(Intent(this, BatteryWidgetConfigureActivity::class.java))
        finish()
        return
    }

    override fun onResume() {
        super.onResume() 
    }

 
} 