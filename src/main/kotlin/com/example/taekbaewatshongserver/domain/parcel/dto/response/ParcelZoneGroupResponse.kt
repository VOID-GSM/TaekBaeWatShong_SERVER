package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone

data class ParcelZoneGroupResponse(
    val zones: List<ZoneGroupDetail>
) {
    data class ZoneGroupDetail(
        val zone: Zone,
        val count: Int,
        val parcels: List<ParcelSimpleDetail>
    )

    data class ParcelSimpleDetail(
        val id: Long,
        val invoiceNumber: String,
        val alias: String,
        val ownerName: String,
        val arrivedAt: java.time.LocalDateTime?,
        val unclaimedDays: Int
    )
}