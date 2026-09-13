package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AdbDevice(
    val host: String,
    val port: Int,
    val isConnected: Boolean = false,
    val lastConnected: Long = 0L
)
