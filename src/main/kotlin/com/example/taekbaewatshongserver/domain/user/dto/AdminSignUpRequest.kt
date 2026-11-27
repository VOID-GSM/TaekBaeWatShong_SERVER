package com.example.taekbaewatshongserver.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 회원가입 요청")
data class AdminSignUpRequest(
    @field:Schema(description = "이메일. 이미 ADMIN인 계정이거나, 유효한 초대 코드(`inviteCode`)를 함께 보내야 관리자로 등록된다.", example = "admin@gsm.hs.kr", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
    @field:Schema(description = "비밀번호 (8자 이상, UTF-8 기준 72바이트 이하)", example = "password1234", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String,
    @field:Schema(description = "이름 (공백 불가)", example = "관리자", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "관리자 초대 코드. 처음 관리자로 등록할 때 필요하다. 하이픈·공백·대소문자는 무시한다.", example = "K7Q2-M9XA", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    val inviteCode: String? = null,
)
