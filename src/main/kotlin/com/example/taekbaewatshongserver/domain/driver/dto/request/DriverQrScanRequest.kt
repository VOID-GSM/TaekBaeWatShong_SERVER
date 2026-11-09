package com.example.taekbaewatshongserver.domain.driver.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "택배기사 QR 스캔 요청")
data class DriverQrScanRequest(
    @field:Schema(description = "QR 코드에 담긴 토큰 값", example = "8d3c2f4e-1b6a-4c7d-9e0f-2a3b4c5d6e7f", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    val qrToken: String,
)
