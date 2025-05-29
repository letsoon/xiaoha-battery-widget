package com.xiaoha.batterywidget.api

data class BatteryResponse(
    val code: Int,
    val message: String,
    val data: BatteryData
)

data class BatteryData(
    val batteryLife: Int,
    val reportTime: Long
) 