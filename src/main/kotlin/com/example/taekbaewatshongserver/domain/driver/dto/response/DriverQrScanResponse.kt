package com.example.taekbaewatshongserver.domain.driver.dto.response

import com.example.taekbaewatshongserver.domain.driver.entity.DriverScanLog
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "택배기사 QR 스캔 결과")
data class DriverQrScanResponse(
    @field:Schema(description = "택배기사 ID", example = "1")
    val driverId: Long,
    @field:Schema(description = "택배기사 이름", example = "김택배")
    val driverName: String,
    @field:Schema(description = "소속 택배사", example = "CJ대한통운")
    val deliveryCompany: String,
    @field:Schema(description = "스캔 시각", example = "2026-11-09T14:30:00")
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
