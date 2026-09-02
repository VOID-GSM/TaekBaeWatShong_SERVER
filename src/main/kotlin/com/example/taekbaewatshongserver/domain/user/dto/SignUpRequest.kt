package com.example.taekbaewatshongserver.domain.user.dto

import com.example.taekbaewatshongserver.domain.user.entity.Role

data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: Role,
)
