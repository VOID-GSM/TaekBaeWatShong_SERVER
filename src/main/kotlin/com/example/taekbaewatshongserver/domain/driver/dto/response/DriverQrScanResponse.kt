package com.example.taekbaewatshongserver.domain.driver.dto.response

import com.example.taekbaewatshongserver.domain.driver.entity.DriverScanLog
import java.time.LocalDateTime

data class DriverQrScanResponse(
    val driverId: Long,
    val driverName: String,
    val deliveryCompany: String,
    val scannedAt: LocalDateTime,
) {
    companion object {
        fun from(log: DriverScanLog): DriverQrScanResponse = DriverQrScanResponse(
            driverId = log.driver.id,
            driverName = log.driver.driverName,
            deliveryCompany = log.driver.deliveryCompany,
            scannedAt = log.scannedAt,
        )
    }
}
