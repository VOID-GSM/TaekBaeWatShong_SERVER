package com.example.taekbaewatshongserver.domain.parcel.entity

enum class ParcelStatus {
    PENDING,  // 도착 예정
    ARRIVED,  // 도착 완료
    CLAIMED   // 수령 완료
}