package com.example.taekbaewatshongserver.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "JWT 발급 응답")
data class TokenResponse(
    @field:Schema(description = "JWT 액세스 토큰 (유효 1시간). `Authorization: Bearer {token}` 헤더로 보낸다.", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123")
    val token: String,
)
