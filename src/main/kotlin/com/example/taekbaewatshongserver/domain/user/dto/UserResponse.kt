package com.example.taekbaewatshongserver.domain.user.dto

import com.example.taekbaewatshongserver.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 정보")
data class UserResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long?,
    @field:Schema(description = "이메일", example = "s25000@gsm.hs.kr")
    val email: String,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "역할. STUDENT(학생) / TEACHER(선생님) / ADMIN(관리자·택배기사)", example = "STUDENT", allowableValues = ["STUDENT", "TEACHER", "ADMIN"])
    val role: String,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
    }
}
