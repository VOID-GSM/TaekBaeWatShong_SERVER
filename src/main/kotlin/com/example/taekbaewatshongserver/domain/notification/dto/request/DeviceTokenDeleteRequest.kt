package com.example.taekbaewatshongserver.domain.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "디바이스 토큰 삭제 요청")
data class DeviceTokenDeleteRequest(
    @field:Schema(description = "삭제할 기기 토큰 (등록할 때 보낸 값 그대로)", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]", requiredMode = Schema.RequiredMode.REQUIRED)
    val deviceToken: String,
)
