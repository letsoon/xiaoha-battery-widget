package com.xiaoha.batterywidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import android.app.AlarmManager
import android.os.Build
import android.provider.Settings

class BatteryWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var batteryNoEdit: EditText
    private lateinit var cityCodeEdit: EditText
    private lateinit var baseUrlEdit: EditText
    private lateinit var refreshIntervalSpinner: Spinner
    private lateinit var addButton: Button
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.battery_widget_configure)

        // 获取小部件ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // 初始化视图引用
        initViews()


        // 在后台初始化
        lifecycleScope.launch {
            try {
                // 在后台线程准备数据
                val initData = withContext(Dispatchers.Default) {
                    val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
                    val savedBatteryNo = prefs.getString("batteryNo_$appWidgetId", "")
                    val savedCityCode = prefs.getString("cityCode_$appWidgetId", "0755")
                    val savedRefreshInterval = prefs.getInt("refreshInterval_$savedBatteryNo", 30)
                    val baseUrl = prefs.getString("baseUrl", "https://xiaoha.linkof.link")
                    val refreshIntervals = resources.getStringArray(R.array.refresh_intervals)
                    val intervals = resources.getIntArray(R.array.refresh_intervals)
                    val position = intervals.indexOf(savedRefreshInterval)
                    InitData(
                        savedBatteryNo.toString(),
                        savedCityCode.toString(),
                        baseUrl.toString(),
                        position,
                        refreshIntervals,
                    )
                }

                // 在主线程更新UI
                withContext(Dispatchers.Main) {
                    setupViews(initData)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BatteryWidgetConfigureActivity, 
                        "初始化失败：${e.message}", 
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun initViews() {
        batteryNoEdit = findViewById(R.id.battery_no_edit)
        baseUrlEdit = findViewById(R.id.base_url_edit)
        cityCodeEdit = findViewById(R.id.city_code_edit)
        refreshIntervalSpinner = findViewById(R.id.refresh_interval_spinner)
        addButton = findViewById(R.id.add_button)

        // 添加 GitHub 链接点击事件
        findViewById<View>(R.id.github_container).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                   data = android.net.Uri.parse(getString(R.string.github_link))
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开链接：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setupViews(initData: InitData) {
        // 设置已保存的值
        batteryNoEdit.setText(initData.batteryNo)
        cityCodeEdit.setText(initData.cityCode)
        baseUrlEdit.setText(initData.baseUrl)

        // 设置刷新间隔选项
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            initData.refreshIntervals
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        refreshIntervalSpinner.adapter = adapter

        // 设置已保存的刷新间隔
        if (initData.refreshIntervalPosition != -1) {
            refreshIntervalSpinner.setSelection(initData.refreshIntervalPosition)
        }

        // 设置保存按钮点击事件
        addButton.setOnClickListener {
            saveConfiguration()
        }
    }

    private fun saveConfiguration() {
        checkAndRequestExactAlarmPermission()
        val batteryNo = batteryNoEdit.text.toString().trim()
        val cityCode = cityCodeEdit.text.toString().trim().let {
            if (it.isEmpty()) "0755" else it
        }
        
        if (batteryNo.isEmpty()) {
            AlertDialog.Builder(this@BatteryWidgetConfigureActivity)
                .setTitle("保存失败")
                .setMessage("请输入电池编号")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        // 禁用输入和按钮
        setInputsEnabled(false)
        addButton.text = "保存中..."

        lifecycleScope.launch {
            try {
                // 保存配置
                withContext(Dispatchers.IO) {
                    val prefs = getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    
                    // 保存电池号和城市代码
                    editor.putString("batteryNo_$appWidgetId", batteryNo)
                    editor.putString("cityCode_$appWidgetId", cityCode)
                    
                    // 保存刷新间隔
                    val intervals = resources.getIntArray(R.array.refresh_interval_values)
                    val selectedInterval = intervals[refreshIntervalSpinner.selectedItemPosition]
                    editor.putInt("refreshInterval_$batteryNo", selectedInterval)
                    
                    editor.apply()

                    // 更新小部件
                    val appWidgetManager = AppWidgetManager.getInstance(this@BatteryWidgetConfigureActivity)
                    val widget = BatteryWidget()
                    widget.onUpdate(this@BatteryWidgetConfigureActivity, appWidgetManager, intArrayOf(appWidgetId))
                }

                // 设置结果并关闭活动
                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@BatteryWidgetConfigureActivity)
                        .setTitle("保存失败")
                        .setMessage(e.message ?: "未知错误")
                        .setPositiveButton("确定", null)
                        .show()
                    setInputsEnabled(true)
                    addButton.text = getString(R.string.add_widget)
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        batteryNoEdit.isEnabled = enabled
        cityCodeEdit.isEnabled = enabled
        refreshIntervalSpinner.isEnabled = enabled
        addButton.isEnabled = enabled
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 跳转系统设置界面让用户授权
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private data class InitData(
        val batteryNo: String,
        val cityCode: String,
        val baseUrl: String,
        val refreshIntervalPosition: Int,
        val refreshIntervals: Array<String>
    )
} 