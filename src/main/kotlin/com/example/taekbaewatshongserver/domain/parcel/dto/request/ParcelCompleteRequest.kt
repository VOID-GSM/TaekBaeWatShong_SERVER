package com.example.taekbaewatshongserver.domain.parcel.dto.request

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "택배 도착 처리 요청")
data class ParcelCompleteRequest(
    @field:Schema(description = "스캔한 운송장 번호", example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    val invoiceNumber: String,
    @field:Schema(description = "함께 배정할 보관 구역 (선택). A~F", example = "A")
    val zone: Zone? = null,
)
