package com.example.taekbaewatshongserver.domain.parcel.dto.request

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "보관 구역 배정 요청")
data class ParcelZoneAssignRequest(
    @field:Schema(description = "배정할 보관 구역. A~C는 1번 선반 상·중·하단, D~F는 2번 선반 상·중·하단", example = "B", requiredMode = Schema.RequiredMode.REQUIRED)
    val zone: Zone,
)
