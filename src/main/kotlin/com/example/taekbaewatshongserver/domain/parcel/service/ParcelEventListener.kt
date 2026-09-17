package com.example.taekbaewatshongserver.domain.parcel.service

import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import com.example.taekbaewatshongserver.domain.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ParcelEventListener(
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelRegisteredEvent(event: ParcelRegisteredEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        notificationService.create(
            recipient = owner,
            parcel = parcel,
            type = NotificationType.REGISTERED,
            title = "택배가 등록되었습니다",
            message = "[${parcel.alias}] 택배 등록이 완료되었습니다. 운송장번호: ${parcel.invoiceNumber}"
        )
        log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배 등록이 완료되었습니다.", owner.name, parcel.alias)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelArrivedEvent(event: ParcelArrivedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        notificationService.create(
            recipient = owner,
            parcel = parcel,
            type = NotificationType.ARRIVED,
            title = "택배가 도착했습니다",
            message = "[${parcel.alias}] 택배가 도착했습니다. (${parcel.zone?.description ?: "구역 미배정"})"
        )
        log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배가 {} 구역에 도착했습니다.", owner.name, parcel.alias, parcel.zone?.name ?: "미배정")
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelZoneAssignedEvent(event: ParcelZoneAssignedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        notificationService.create(
            recipient = owner,
            parcel = parcel,
            type = NotificationType.ZONE_ASSIGNED,
            title = "보관 구역이 배정되었습니다",
            message = "[${parcel.alias}] 택배가 ${parcel.zone?.description ?: "미배정"} 에 배정되었습니다."
        )
        log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배가 {} 구역에 배정되었습니다.", owner.name, parcel.alias, parcel.zone?.name)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelClaimedEvent(event: ParcelClaimedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        notificationService.create(
            recipient = owner,
            parcel = parcel,
            type = NotificationType.CLAIMED,
            title = "택배를 수령했습니다",
            message = "[${parcel.alias}] 택배 수령이 완료되었습니다."
        )
        log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배 수령이 완료되었습니다.", owner.name, parcel.alias)
    }
}