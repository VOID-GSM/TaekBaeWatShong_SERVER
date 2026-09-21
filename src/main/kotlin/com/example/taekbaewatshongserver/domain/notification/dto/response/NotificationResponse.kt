package com.example.taekbaewatshongserver.domain.notification.dto.response

import com.example.taekbaewatshongserver.domain.notification.entity.Notification
import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val type: String,
    val title: String,
    val message: String,
    val parcelId: Long,
    val unclaimedDays: Int? = null,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = notification.id,
            type = notification.type.name,
            title = notification.title,
            message = notification.message,
            parcelId = notification.parcel.id,
            unclaimedDays = if (notification.type == NotificationType.UNCLAIMED_REMINDER) notification.unclaimedDays else null,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
        )
    }
}
