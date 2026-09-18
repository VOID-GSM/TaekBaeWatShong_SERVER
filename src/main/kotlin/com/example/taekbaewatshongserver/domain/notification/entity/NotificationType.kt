package com.example.taekbaewatshongserver.domain.notification.entity

enum class NotificationType {
    REGISTERED,          // 택배 등록 알림
    ARRIVED,              // 택배 도착 알림
    ZONE_ASSIGNED,        // 구역 배정 알림
    CLAIMED,               // 택배 회수(가지고 감) 알림
    UNCLAIMED_REMINDER    // 방지 일수 기반 수령 재알림
}
