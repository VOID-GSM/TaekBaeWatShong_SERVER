package com.example.taekbaewatshongserver.domain.driver.dto.request

import jakarta.validation.constraints.NotBlank

data class DriverQrScanRequest(
    @field:NotBlank
    val qrToken: String,
)
