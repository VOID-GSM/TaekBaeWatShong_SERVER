package com.example.taekbaewatshongserver.domain.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그아웃 요청")
data class LogoutRequest(
    @field:Schema(
        description = "이 기기에 저장된 refresh token (43자). 만료됐거나 이미 폐기된 값이어도 보내면 된다.",
        example = "q3J9vX0bA1c2D3e4F5g6H7i8J9k0L1m2N3o4P5q6R7s",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refreshToken: String,
)
