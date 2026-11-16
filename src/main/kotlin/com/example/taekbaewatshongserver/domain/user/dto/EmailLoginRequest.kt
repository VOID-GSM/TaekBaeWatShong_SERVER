package com.example.taekbaewatshongserver.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이메일 로그인 요청")
data class EmailLoginRequest(
    @field:Schema(description = "이메일", example = "s25000@gsm.hs.kr", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
    @field:Schema(description = "비밀번호", example = "password1234", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String,
)
