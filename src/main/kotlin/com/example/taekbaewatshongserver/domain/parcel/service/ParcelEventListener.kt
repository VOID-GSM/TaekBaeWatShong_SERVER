package com.example.taekbaewatshongserver.domain.parcel.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ParcelEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelArrivedEvent(event: ParcelArrivedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        log.info("[알림 발생] 수신자: {} | 내용: [{}] 택배가 {} 구역에 도착했습니다.", owner.name, parcel.alias, parcel.zone?.name ?: "미배정")
    }
}