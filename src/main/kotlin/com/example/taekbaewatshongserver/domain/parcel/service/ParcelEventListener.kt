package com.example.taekbaewatshongserver.domain.parcel.service

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ParcelEventListener {

    @Async
    @EventListener
    fun handleParcelArrivedEvent(event: ParcelArrivedEvent) {
        val parcel = event.parcel
        val owner = parcel.owner

        // 추후 웹 푸시 알림 기능 구현
        println("[알림 발생] 수신자: ${owner.name} | 내용: [${parcel.alias}] 택배가 ${parcel.zone?.name ?: "미배정"} 구역에 도착했습니다.")
    }
}