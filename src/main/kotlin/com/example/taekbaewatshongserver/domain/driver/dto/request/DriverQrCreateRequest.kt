package com.example.taekbaewatshongserver.domain.driver.dto.request

import jakarta.validation.constraints.NotBlank

data class DriverQrCreateRequest(
    @field:NotBlank
    val driverName: String,
    @field:NotBlank
    val deliveryCompany: String,
)
