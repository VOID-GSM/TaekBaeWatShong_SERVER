package com.example.taekbaewatshongserver.domain.parcel.service

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ParcelEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParcelArrivedEvent(event: ParcelArrivedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        // DB 트랜잭션 커밋이 성공적으로 완료된 후 별도 스레드에서 비동기로 실행됨
        println("[알림 발생] 수신자: ${owner.name} | 내용: [${parcel.alias}] 택배가 ${parcel.zone?.name ?: "미배정"} 구역에 도착했습니다.")
    }
}