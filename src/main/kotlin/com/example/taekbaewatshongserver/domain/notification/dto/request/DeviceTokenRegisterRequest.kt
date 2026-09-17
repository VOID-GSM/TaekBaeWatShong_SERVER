package com.example.taekbaewatshongserver.domain.notification.dto.request

data class DeviceTokenRegisterRequest(
    val deviceToken: String,
    val platform: String
)
