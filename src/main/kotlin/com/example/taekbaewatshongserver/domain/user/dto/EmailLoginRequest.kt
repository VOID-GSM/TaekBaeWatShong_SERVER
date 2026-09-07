package com.example.taekbaewatshongserver.domain.user.dto

data class EmailLoginRequest(
    val email: String,
    val password: String,
)
