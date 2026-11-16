package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "택배 정보")
data class ParcelResponse(
    @field:Schema(description = "택배 ID", example = "1")
    val id: Long,
    @field:Schema(description = "택배사 이름", example = "CJ대한통운")
    val deliveryCompany: String,
    @field:Schema(description = "운송장 번호", example = "123456789012")
    val invoiceNumber: String,
    @field:Schema(description = "택배 별칭", example = "운동화")
    val alias: String,
    @field:Schema(description = "택배 주인 이름", example = "홍길동")
    val ownerName: String? = null,
    @field:Schema(description = "보관 구역 (미배정이면 null)", example = "A")
    val zone: Zone? = null,
    @field:Schema(description = "택배 상태. PENDING(도착 예정) / ARRIVED(도착 완료) / CLAIMED(수령 완료)", example = "ARRIVED")
    val status: ParcelStatus,
    @field:Schema(description = "도착 처리 시각 (도착 전이면 null)", example = "2026-11-09T14:30:00")
    val arrivedAt: LocalDateTime? = null,
    @field:Schema(description = "수령 시각 (수령 전이면 null)")
    val claimedAt: LocalDateTime? = null,
    @field:Schema(description = "도착 후 찾아가지 않은 일수 (날짜 기준). ARRIVED가 아니면 0", example = "2")
    val unclaimedDays: Int,
    @field:Schema(description = "등록 시각", example = "2026-11-07T09:00:00")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(parcel: Parcel): ParcelResponse = ParcelResponse(
            id = parcel.id,
            deliveryCompany = parcel.deliveryCompany,
            invoiceNumber = parcel.invoiceNumber,
            alias = parcel.alias,
            ownerName = parcel.owner.name,
            zone = parcel.zone,
            status = parcel.status,
            arrivedAt = parcel.arrivedAt,
            claimedAt = parcel.claimedAt,
            unclaimedDays = parcel.unclaimedDays,
            createdAt = parcel.createdAt,
        )
    }
}
