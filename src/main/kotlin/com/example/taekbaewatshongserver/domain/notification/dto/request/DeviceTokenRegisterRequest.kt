package com.example.taekbaewatshongserver.domain.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "디바이스 토큰 등록 요청")
data class DeviceTokenRegisterRequest(
    @field:Schema(
        description = "Expo 푸시 토큰. 앱에서 getExpoPushTokenAsync()로 받은 값을 그대로 보낸다. " +
            "ExponentPushToken[...] 형식이 아니면 400으로 거절된다.",
        example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val deviceToken: String,
    @field:Schema(description = "기기 플랫폼 (대문자 정확히 일치해야 함)", example = "ANDROID", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ["ANDROID", "IOS", "WEB"])
    val platform: String,
)
