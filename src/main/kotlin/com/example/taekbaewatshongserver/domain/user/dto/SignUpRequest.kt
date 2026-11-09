package com.example.taekbaewatshongserver.domain.user.dto

import com.example.taekbaewatshongserver.domain.user.entity.Role
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 요청")
data class SignUpRequest(
    @field:Schema(description = "이메일", example = "s25000@gsm.hs.kr", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
    @field:Schema(description = "비밀번호 (8자 이상, UTF-8 기준 72바이트 이하)", example = "password1234", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String,
    @field:Schema(description = "이름 (공백 불가)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "역할. STUDENT(학생) 또는 TEACHER(선생님)만 가능", example = "STUDENT", requiredMode = Schema.RequiredMode.REQUIRED)
    val role: Role,
    @field:Schema(description = "학번 4자리 숫자. STUDENT면 필수, TEACHER면 무시된다.", example = "2101")
    val studentNumber: String? = null,
)
