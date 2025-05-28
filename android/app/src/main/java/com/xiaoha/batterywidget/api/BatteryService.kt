package com.xiaoha.batterywidget.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BatteryService {
    @GET("/")
    fun getBatteryInfo(
        @Query("batteryNo") batteryNo: String,
        @Query("format") format: String = "json"
    ): Call<BatteryResponse>
} 