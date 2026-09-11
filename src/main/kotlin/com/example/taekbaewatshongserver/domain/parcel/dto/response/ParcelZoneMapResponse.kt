package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone

data class ParcelZoneMapResponse(
    val imageUrl: String,
    val zones: List<ZoneDetail>
) {
    data class ZoneDetail(
        val zone: Zone,
        val description: String
    )
}