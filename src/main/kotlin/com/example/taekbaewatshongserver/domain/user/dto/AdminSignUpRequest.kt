package com.example.taekbaewatshongserver.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 회원가입 요청")
data class AdminSignUpRequest(
    @field:Schema(description = "이메일. 서버의 관리자 이메일 목록(`ADMIN_EMAILS`)에 있어야 한다.", example = "admin@gsm.hs.kr", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
    @field:Schema(description = "비밀번호 (8자 이상, UTF-8 기준 72바이트 이하)", example = "password1234", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String,
    @field:Schema(description = "이름 (공백 불가)", example = "관리자", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)
