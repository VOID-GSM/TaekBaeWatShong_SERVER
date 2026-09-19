package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import java.time.LocalDateTime

data class ParcelClaimResponse(
    val id: Long,
    val invoiceNumber: String,
    val alias: String,
    val zone: Zone?,
    val status: String,
    val claimedAt: LocalDateTime?,
    val unclaimedDays: Int,
    val message: String = "회수가 완료되었습니다!"
) {
    companion object {
        fun from(parcel: Parcel): ParcelClaimResponse {
            return ParcelClaimResponse(
                id = parcel.id,
                invoiceNumber = parcel.invoiceNumber,
                alias = parcel.alias,
                zone = parcel.zone,
                status = parcel.status.name,
                claimedAt = parcel.claimedAt,
                unclaimedDays = parcel.unclaimedDays
            )
        }
    }
}