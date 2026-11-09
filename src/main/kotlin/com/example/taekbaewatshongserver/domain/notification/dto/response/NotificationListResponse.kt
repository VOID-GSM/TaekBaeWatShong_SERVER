package com.example.taekbaewatshongserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 목록 (커서 페이지)")
data class NotificationListResponse(
    @field:Schema(description = "안 읽은 알림 전체 개수", example = "3")
    val unreadCount: Int,
    @field:Schema(description = "알림 목록 (최신순)")
    val notifications: List<NotificationResponse>,
    @field:Schema(description = "다음 페이지 요청 시 `cursor`로 보낼 값. 마지막 페이지면 null", example = "101")
    val nextCursor: Long?,
)
