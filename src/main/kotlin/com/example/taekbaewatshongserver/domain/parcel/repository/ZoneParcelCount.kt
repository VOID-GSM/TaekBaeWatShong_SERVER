package com.example.taekbaewatshongserver.domain.parcel.repository

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone

data class ZoneParcelCount(
    val zone: Zone,
    val count: Long,
)
