package com.example.taekbaewatshongserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 읽음 처리 결과")
data class NotificationReadResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "읽음 여부 (항상 true)", example = "true")
    val isRead: Boolean,
    @field:Schema(description = "처리 후 남은 안 읽은 알림 개수", example = "2")
    val unreadCount: Int,
)
