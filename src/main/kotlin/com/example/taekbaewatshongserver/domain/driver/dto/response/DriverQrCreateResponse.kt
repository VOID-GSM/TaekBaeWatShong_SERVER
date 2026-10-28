package com.example.taekbaewatshongserver.domain.driver.dto.response

import com.example.taekbaewatshongserver.domain.driver.entity.Driver
import java.time.LocalDateTime

data class DriverQrCreateResponse(
    val id: Long,
    val driverName: String,
    val deliveryCompany: String,
    val qrToken: String,
    val qrImageUrl: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(driver: Driver, qrImageUrl: String): DriverQrCreateResponse = DriverQrCreateResponse(
            id = driver.id,
            driverName = driver.driverName,
            deliveryCompany = driver.deliveryCompany,
            qrToken = driver.qrToken,
            qrImageUrl = qrImageUrl,
            createdAt = driver.createdAt,
        )
    }
}
