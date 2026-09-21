package com.example.taekbaewatshongserver.domain.notification.dto.response

data class NotificationListResponse(
    val unreadCount: Int,
    val notifications: List<NotificationResponse>,
    val nextCursor: Long?,
)
