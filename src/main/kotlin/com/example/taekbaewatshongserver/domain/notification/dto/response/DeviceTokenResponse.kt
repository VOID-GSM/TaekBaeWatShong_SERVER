package com.example.taekbaewatshongserver.domain.notification.dto.response

import com.example.taekbaewatshongserver.domain.notification.entity.DeviceToken
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "등록된 디바이스 토큰")
data class DeviceTokenResponse(
    @field:Schema(description = "디바이스 토큰 ID", example = "1")
    val id: Long,
    @field:Schema(description = "기기 플랫폼", example = "ANDROID", allowableValues = ["ANDROID", "IOS", "WEB"])
    val platform: String,
    @field:Schema(description = "최초 등록 시각", example = "2026-11-09T14:30:00")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(deviceToken: DeviceToken): DeviceTokenResponse = DeviceTokenResponse(
            id = deviceToken.id,
            platform = deviceToken.platform.name,
            createdAt = deviceToken.createdAt,
        )
    }
}
