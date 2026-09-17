package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import com.example.taekbaewatshongserver.domain.notification.repository.NotificationRepository
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.repository.ParcelRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UnclaimedReminderScheduler(
    private val parcelRepository: ParcelRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    @Value("\${notification.unclaimed-reminder.days:1,3,5}") reminderDaysProperty: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val reminderDays = reminderDaysProperty
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { DEFAULT_REMINDER_DAYS }

    companion object {
        private val DEFAULT_REMINDER_DAYS = listOf(1, 3, 5)
    }

    @Transactional
    @Scheduled(cron = "\${notification.unclaimed-reminder.cron:0 0 8 * * *}")
    fun sendUnclaimedReminders() {
        val arrivedParcels = parcelRepository.findAllByStatusOrderByCreatedAtDesc(ParcelStatus.ARRIVED)

        for (parcel in arrivedParcels) {
            val unclaimedDays = parcel.unclaimedDays
            if (unclaimedDays !in reminderDays) continue

            val alreadySent = notificationRepository.existsByParcelAndTypeAndUnclaimedDays(
                parcel = parcel,
                type = NotificationType.UNCLAIMED_REMINDER,
                unclaimedDays = unclaimedDays
            )
            if (alreadySent) continue

            log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배가 도착 후 {}일째 미수령 상태입니다.", parcel.owner.name, parcel.alias, unclaimedDays)
            notificationService.create(
                recipient = parcel.owner,
                parcel = parcel,
                type = NotificationType.UNCLAIMED_REMINDER,
                title = "택배 수령이 지연되고 있습니다",
                message = "[${parcel.alias}] 택배가 도착 후 ${unclaimedDays}일째 보관 중입니다. 수령해주세요.",
                unclaimedDays = unclaimedDays
            )
        }
    }
}
