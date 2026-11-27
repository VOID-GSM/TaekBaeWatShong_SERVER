package com.example.taekbaewatshongserver.domain.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 재발급 요청")
data class TokenRefreshRequest(
    @field:Schema(
        description = "가장 최근에 받은 refresh token (43자). 로그인 또는 직전 재발급 응답의 `refreshToken` 값을 그대로 보낸다.",
        example = "q3J9vX0bA1c2D3e4F5g6H7i8J9k0L1m2N3o4P5q6R7s",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refreshToken: String,
)
