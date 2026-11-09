package com.example.taekbaewatshongserver.domain.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "디바이스 토큰 등록 요청")
data class DeviceTokenRegisterRequest(
    @field:Schema(description = "푸시 알림용 기기 토큰 (FCM 토큰 등)", example = "fGx1c2VyLXRva2VuOkFQQTkxYkg", requiredMode = Schema.RequiredMode.REQUIRED)
    val deviceToken: String,
    @field:Schema(description = "기기 플랫폼 (대문자 정확히 일치해야 함)", example = "ANDROID", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ["ANDROID", "IOS", "WEB"])
    val platform: String,
)
