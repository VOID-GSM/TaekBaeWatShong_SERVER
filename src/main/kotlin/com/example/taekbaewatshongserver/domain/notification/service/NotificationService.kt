package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationListResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationResponse
import com.example.taekbaewatshongserver.domain.notification.repository.NotificationRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_SIZE = 20
private const val MAX_SIZE = 100

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository
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
            nextCursor = nextCursor
        )
    }
}
