package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "보관 구역 배정 결과")
data class ParcelZoneAssignResponse(
    @field:Schema(description = "택배 ID", example = "1")
    val id: Long,
    @field:Schema(description = "운송장 번호", example = "123456789012")
    val invoiceNumber: String,
    @field:Schema(description = "배정된 보관 구역", example = "B")
    val zone: Zone?,
    @field:Schema(description = "택배 상태. PENDING / ARRIVED / CLAIMED", example = "ARRIVED")
    val status: ParcelStatus,
    @field:Schema(description = "도착 처리 시각 (도착 전이면 null)", example = "2026-11-09T14:30:00")
    val arrivedAt: LocalDateTime?,
) {
    companion object {
        fun from(parcel: Parcel): ParcelZoneAssignResponse = ParcelZoneAssignResponse(
            id = parcel.id,
            invoiceNumber = parcel.invoiceNumber,
            zone = parcel.zone,
            status = parcel.status,
            arrivedAt = parcel.arrivedAt,
        )
    }
}
