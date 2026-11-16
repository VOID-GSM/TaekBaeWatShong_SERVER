package com.example.taekbaewatshongserver.domain.driver.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "택배기사 QR 발급 요청")
data class DriverQrCreateRequest(
    @field:Schema(description = "택배기사 이름 (공백 불가)", example = "김택배", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    val driverName: String,
    @field:Schema(description = "소속 택배사 (공백 불가)", example = "CJ대한통운", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    val deliveryCompany: String,
)
