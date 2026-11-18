package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType

/**
 * 알림이 DB에 저장(커밋)된 뒤 푸시 발송을 위해 발행되는 이벤트.
 * 비동기 리스너에서 지연 로딩 없이 쓸 수 있도록 필요한 값을 모두 담는다.
 */
data class NotificationCreatedEvent(
    val notificationId: Long,
    val recipientId: Long,
    val parcelId: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
)
