package com.example.taekbaewatshongserver.domain.driver.dto.response

import com.example.taekbaewatshongserver.domain.driver.entity.Driver
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "발급된 택배기사 QR")
data class DriverQrCreateResponse(
    @field:Schema(description = "택배기사 ID", example = "1")
    val id: Long,
    @field:Schema(description = "택배기사 이름", example = "김택배")
    val driverName: String,
    @field:Schema(description = "소속 택배사", example = "CJ대한통운")
    val deliveryCompany: String,
    @field:Schema(description = "QR 코드에 담긴 토큰 값", example = "8d3c2f4e-1b6a-4c7d-9e0f-2a3b4c5d6e7f")
    val qrToken: String,
    @field:Schema(description = "QR 이미지 (300×300 PNG, data URL)", example = "data:image/png;base64,iVBORw0KGgo...")
    val qrImageUrl: String,
    @field:Schema(description = "발급 시각", example = "2026-11-09T14:30:00")
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
