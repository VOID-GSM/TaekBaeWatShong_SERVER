package com.example.taekbaewatshongserver.domain.parcel.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "택배 목록")
data class ParcelListResponse(
    @field:Schema(description = "택배 목록 (등록 최신순)")
    val parcels: List<ParcelResponse>,
)
