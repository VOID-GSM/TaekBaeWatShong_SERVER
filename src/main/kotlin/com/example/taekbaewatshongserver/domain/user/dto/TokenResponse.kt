package com.example.taekbaewatshongserver.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 발급 응답 (액세스 토큰 + refresh token)")
data class TokenResponse(
    @field:Schema(
        description = "JWT 액세스 토큰 (유효 1시간). `Authorization: Bearer {token}` 헤더로 보낸다.",
        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val token: String,
    @field:Schema(
        description = "refresh token (43자). 액세스 토큰이 만료되면 POST /auth/refresh로 보낸다. 마지막 재발급 후 30일 동안 쓰지 않으면 만료된다. 앱은 SecureStore에 저장한다.",
        example = "q3J9vX0bA1c2D3e4F5g6H7i8J9k0L1m2N3o4P5q6R7s",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refreshToken: String,
)
