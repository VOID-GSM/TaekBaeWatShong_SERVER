package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import java.time.LocalDateTime

data class ParcelResponse(
    val id: Long,
    val deliveryCompany: String,
    val invoiceNumber: String,
    val alias: String,
    val ownerName: String? = null,
    val zone: Zone? = null,
    val status: ParcelStatus,
    val arrivedAt: LocalDateTime? = null,
    val claimedAt: LocalDateTime? = null,
    val unclaimedDays: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(parcel: Parcel): ParcelResponse {
            return ParcelResponse(
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
                createdAt = parcel.createdAt
            )
        }
    }
}