package com.example.taekbaewatshongserver.domain.notification.dto.response

import com.example.taekbaewatshongserver.domain.notification.entity.DeviceToken
import java.time.LocalDateTime

data class DeviceTokenResponse(
    val id: Long,
    val platform: String,
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
