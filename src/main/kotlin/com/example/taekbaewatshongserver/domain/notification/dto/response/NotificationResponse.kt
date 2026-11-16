package com.example.taekbaewatshongserver.domain.notification.dto.response

import com.example.taekbaewatshongserver.domain.notification.entity.Notification
import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림")
data class NotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "알림 종류. REGISTERED(택배 등록) / ARRIVED(도착) / ZONE_ASSIGNED(구역 배정) / CLAIMED(수령) / UNCLAIMED_REMINDER(미수령 재알림)", example = "ARRIVED", allowableValues = ["REGISTERED", "ARRIVED", "ZONE_ASSIGNED", "CLAIMED", "UNCLAIMED_REMINDER"])
    val type: String,
    @field:Schema(description = "알림 제목", example = "택배가 도착했습니다")
    val title: String,
    @field:Schema(description = "알림 본문", example = "[운동화] 택배가 도착했습니다. (1번 선반 - 1단 (상단))")
    val message: String,
    @field:Schema(description = "관련 택배 ID", example = "1")
    val parcelId: Long,
    @field:Schema(description = "미수령 일수. UNCLAIMED_REMINDER일 때만 값이 있고 나머지는 null")
    val unclaimedDays: Int? = null,
    @field:Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,
    @field:Schema(description = "알림 생성 시각", example = "2026-11-09T14:30:00")
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
