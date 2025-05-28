package com.xiaoha.batterywidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.net.URL
import kotlin.coroutines.CoroutineContext

class BatteryWidgetConfigureActivity : AppCompatActivity(), CoroutineScope {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var batteryNoEdit: EditText
    private lateinit var cityCodeEdit: EditText
    private lateinit var addButton: Button
    
    override val coroutineContext: CoroutineContext
        get() = lifecycleScope.coroutineContext + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置结果为取消，以防用户直接返回
        setResult(Activity.RESULT_CANCELED)
        
        setContentView(R.layout.battery_widget_configure)

        // 获取小组件ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        batteryNoEdit = findViewById(R.id.battery_no_edit)
        cityCodeEdit = findViewById(R.id.city_code_edit)
        addButton = findViewById(R.id.add_button)
        addButton.setOnClickListener { confirmConfiguration() }

        // 设置默认城市代码
        cityCodeEdit.setText("0755")

        // 加载已保存的电池编号
        lifecycleScope.launch(Dispatchers.IO) {
            val savedBatteryNo = getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)
                .getString("batteryNo_$appWidgetId", "")
            if (!savedBatteryNo.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    batteryNoEdit.setText(savedBatteryNo)
                }
            }
        }
    }

    private fun confirmConfiguration() {
        val batteryNo = batteryNoEdit.text.toString().trim()
        val cityCode = cityCodeEdit.text.toString().trim().let {
            if (it.isEmpty()) "0755" else it
        }
        
        if (batteryNo.isEmpty()) {
            Toast.makeText(this, "请输入电池编号", Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用输入和按钮
        batteryNoEdit.isEnabled = false
        cityCodeEdit.isEnabled = false
        addButton.isEnabled = false
        addButton.text = "验证中..."

        // 验证电池编号并保存配置
        lifecycleScope.launch {
            try {
                // 在后台线程验证电池编号
                val isValid = withContext(Dispatchers.IO) {
                    try {
                        val url = URL("https://xiaoha.deno.dev/?batteryNo=$batteryNo&cityCode=$cityCode&format=json")
                        val response = withTimeout(5000) { // 添加5秒超时
                            url.readText()
                        }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (isValid) {
                    withContext(Dispatchers.IO) {
                        // 保存配置
                        getSharedPreferences("BatteryWidgetPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("batteryNo_$appWidgetId", batteryNo)
                            .putString("cityCode_$appWidgetId", cityCode)
                            .apply()

                        // 更新小组件
                        val appWidgetManager = AppWidgetManager.getInstance(this@BatteryWidgetConfigureActivity)
                        val widget = BatteryWidget()
                        widget.onUpdate(this@BatteryWidgetConfigureActivity, appWidgetManager, intArrayOf(appWidgetId))
                    }

                    // 设置结果并关闭
                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(Activity.RESULT_OK, resultValue)
                    finish()
                } else {
                    showError("无效的电池编号")
                }
            } catch (e: Exception) {
                showError("配置失败：${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            batteryNoEdit.isEnabled = true
            cityCodeEdit.isEnabled = true
            addButton.isEnabled = true
            addButton.text = "添加小组件"
        }
    }
} 