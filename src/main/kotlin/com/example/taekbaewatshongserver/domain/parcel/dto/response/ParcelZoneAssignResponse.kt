package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import java.time.LocalDateTime

data class ParcelZoneAssignResponse(
    val id: Long,
    val invoiceNumber: String,
    val zone: Zone?,
    val status: ParcelStatus,
    val arrivedAt: LocalDateTime?
) {
    companion object {
        fun from(parcel: Parcel): ParcelZoneAssignResponse {
            return ParcelZoneAssignResponse(
                id = parcel.id,
                invoiceNumber = parcel.invoiceNumber,
                zone = parcel.zone,
                status = parcel.status,
                arrivedAt = parcel.arrivedAt
            )
        }
    }
}