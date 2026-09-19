package com.example.taekbaewatshongserver.domain.notification.dto.response

data class NotificationReadResponse(
    val id: Long,
    val isRead: Boolean,
    val unreadCount: Int
)
