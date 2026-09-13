package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchDeviceInfo(
    val host: String,
    val port: Int,
    val model: String = "Wear OS Device",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val batteryLevel: Int? = null,
    val isCharging: Boolean = false,
    val totalApps: Int = 0,
    val userApps: Int = 0,
    val systemApps: Int = 0,
    val runningApps: Int = 0
)
