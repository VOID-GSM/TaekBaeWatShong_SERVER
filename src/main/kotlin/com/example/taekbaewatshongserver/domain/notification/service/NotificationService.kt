package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationListResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationReadResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationResponse
import com.example.taekbaewatshongserver.domain.notification.entity.Notification
import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import com.example.taekbaewatshongserver.domain.notification.repository.NotificationRepository
import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.NotificationException
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_SIZE = 20
private const val MAX_SIZE = 100

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {

    fun getNotifications(user: User, cursor: Long?, size: Int?): NotificationListResponse {
        val pageSize = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val pageable = PageRequest.of(0, pageSize + 1)

        val notifications = if (cursor != null) {
            notificationRepository.findByRecipientAndIdLessThanOrderByIdDesc(user, cursor, pageable)
        } else {
            notificationRepository.findByRecipientOrderByIdDesc(user, pageable)
        }

        val hasNext = notifications.size > pageSize
        val content = if (hasNext) notifications.subList(0, pageSize) else notifications
        val nextCursor = if (hasNext) content.last().id else null

        val unreadCount = notificationRepository.countByRecipientAndIsReadFalse(user)

        return NotificationListResponse(
            unreadCount = unreadCount.toInt(),
            notifications = content.map { NotificationResponse.from(it) },
            nextCursor = nextCursor,
        )
    }

    @Transactional
    fun markAsRead(user: User, notificationId: Long): NotificationReadResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NotificationException.NotFound() }

        if (notification.recipient.id != user.id) {
            throw AccessDeniedException("본인의 알림만 읽음 처리할 수 있습니다.")
        }

        notification.markAsRead()

        val unreadCount = notificationRepository.countByRecipientAndIsReadFalse(user)

        return NotificationReadResponse(
            id = notification.id,
            isRead = notification.isRead,
            unreadCount = unreadCount.toInt(),
        )
    }

    @Transactional
    fun create(
        recipient: User,
        parcel: Parcel,
        type: NotificationType,
        title: String,
        message: String,
        unclaimedDays: Int? = null,
    ): Notification {
        val notification = Notification(
            type = type,
            title = title,
            message = message,
            parcel = parcel,
            recipient = recipient,
            unclaimedDays = unclaimedDays,
        )
        return notificationRepository.save(notification)
    }
}
