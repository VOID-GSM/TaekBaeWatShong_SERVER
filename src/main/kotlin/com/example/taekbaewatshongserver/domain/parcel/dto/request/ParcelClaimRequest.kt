package com.example.taekbaewatshongserver.domain.parcel.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "택배 수령 처리 요청")
data class ParcelClaimRequest(
    @field:Schema(description = "수령할 택배의 운송장 번호", example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    val invoiceNumber: String,
)
