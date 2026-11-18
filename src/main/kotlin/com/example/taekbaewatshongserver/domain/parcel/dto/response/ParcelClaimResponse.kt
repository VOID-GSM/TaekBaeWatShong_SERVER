package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "택배 수령 처리 결과")
data class ParcelClaimResponse(
    @field:Schema(description = "택배 ID", example = "1")
    val id: Long,
    @field:Schema(description = "운송장 번호", example = "123456789012")
    val invoiceNumber: String,
    @field:Schema(description = "택배 별칭", example = "운동화")
    val alias: String,
    @field:Schema(description = "보관돼 있던 구역 (미배정이었으면 null)", example = "A")
    val zone: Zone?,
    @field:Schema(description = "택배 상태 (항상 CLAIMED)", example = "CLAIMED", allowableValues = ["PENDING", "ARRIVED", "CLAIMED"])
    val status: String,
    @field:Schema(description = "수령 시각", example = "2026-11-09T14:30:00")
    val claimedAt: LocalDateTime?,
    @field:Schema(description = "수령 후에는 항상 0", example = "0")
    val unclaimedDays: Int,
    @field:Schema(description = "화면에 보여줄 안내 문구", example = "회수가 완료되었습니다!")
    val message: String = "회수가 완료되었습니다!",
) {
    companion object {
        fun from(parcel: Parcel): ParcelClaimResponse = ParcelClaimResponse(
            id = parcel.id,
            invoiceNumber = parcel.invoiceNumber,
            alias = parcel.alias,
            zone = parcel.zone,
            status = parcel.status.name,
            claimedAt = parcel.claimedAt,
            unclaimedDays = parcel.unclaimedDays,
        )
    }
}
