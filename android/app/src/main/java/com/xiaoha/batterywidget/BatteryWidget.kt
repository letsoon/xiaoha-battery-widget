package com.xiaoha.batterywidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BatteryWidget : AppWidgetProvider() {
    companion object {
        private var logoBitmap: Bitmap? = null
        private val updateJobs = ConcurrentHashMap<Int, Job>()
        private val dateFormat = SimpleDateFormat("M/d HH:mm", Locale.US)
        private const val TAG = "BatteryWidget"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate called for widget IDs: ${appWidgetIds.joinToString()}")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "onDeleted called for widget IDs: ${appWidgetIds.joinToString()}")
        
        appWidgetIds.forEach { appWidgetId ->
            updateJobs[appWidgetId]?.cancel()
            updateJobs.remove(appWidgetId)
            
            // 清除配置
            context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
                .edit()
                .remove("batteryNo_$appWidgetId")
                .apply()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled")
        coroutineScope.cancel()
        updateJobs.clear()
        logoBitmap = null
    }

    private fun decodeLogo(): Bitmap? {
        return try {
            if (logoBitmap == null) {
                val logoBase64 = "iVBORw0KGgoAAAANSUhEUgAAABwAAAAcCAMAAABF0y+mAAAAbFBMVEUAiP4Ah/4AhP4lj/4Agv4Af/6Qv//////R4/8Aff5Uov7w9//p8/9lqf/I3v/w9f+pzv9srP/4/P+82P+Huf7e6v9xsf99tf640/8ylP6hyf9Hnf6dxv8Ag/7k7/8Aev7X5/8Adv5AmP4RjP7GGMZdAAAA9klEQVR4AdXLBwKDIAxA0QSDwb23VKX3v2MZnUfodzAewJ+Gn1noy0T0WuL3aZKSgGJWAkFImaRZltsnK4S1sqrqpOG47aToq2po2pGbdsomi7KaS7Xwmmy8J3O18pgB6za6BZx2lXUH2dvbPGxccO6fJuCq0rW2TTgPKUMxTrYCIeB5daXNSI9LxcxtSU9UULsKjxGfy2a6m3xhw8MwtOpweB/NSkdeh5vjbpHk1Z0WN76T4a7nBR0OQ9bZ8zpRXdJzySQSwxwT2NCoLpLtWaxcCKzPlPou41iCD4kQzZDlk7ZzKag794XgKxSA+jkXpBF+Q/jzHpg8EYrSfggvAAAAAElFTkSuQmCC"
                val logoBytes = Base64.decode(logoBase64, Base64.DEFAULT)
                logoBitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
            }
            logoBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding logo", e)
            null
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d(TAG, "Updating widget ID: $appWidgetId")
        
        val views = RemoteViews(context.packageName, R.layout.battery_widget)
        
        // 设置点击事件
        val intent = Intent(context, BatteryWidgetConfigureActivity::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            appWidgetId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        // 设置 logo
        decodeLogo()?.let { bitmap ->
            views.setImageViewBitmap(R.id.logo, bitmap)
        }

        // 获取配置
        val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
        val batteryNo = prefs.getString("batteryNo_$appWidgetId", "") ?: ""
        val cityCode = prefs.getString("cityCode_$appWidgetId", "0755") ?: "0755"

        if (batteryNo.isNullOrEmpty()) {
            Log.d(TAG, "No battery number configured for widget ID: $appWidgetId")
            updateErrorState(views, "点击配置")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching data for battery: $batteryNo")
                val url = URL("https://xiaoha.deno.dev/?batteryNo=$batteryNo&cityCode=$cityCode&format=json")
                val response = withTimeout(10000) { // 10秒超时
                    url.readText()
                }
                
                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                
                val batteryLife = data.getInt("batteryLife")
                val reportTime = data.getLong("reportTime")
                
                // 格式化时间
                val date = Date(reportTime)
                val formattedTime = dateFormat.format(date)

                Log.d(TAG, "Data fetched: battery=$batteryLife%, time=$formattedTime")

                // 更新UI
                views.setProgressBar(R.id.progress_circle, 100, batteryLife, false)
                views.setTextViewText(R.id.percent_text, "$batteryLife%")
                views.setTextViewText(R.id.battery_no, batteryNo)
                views.setTextViewText(R.id.update_time, formattedTime)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                updateErrorState(views, "更新失败")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun updateErrorState(views: RemoteViews, message: String) {
        views.setProgressBar(R.id.progress_circle, 100, 0, false)
        views.setTextViewText(R.id.percent_text, message)
        views.setTextViewText(R.id.battery_no, "")
        views.setTextViewText(R.id.update_time, "")
    }
} 