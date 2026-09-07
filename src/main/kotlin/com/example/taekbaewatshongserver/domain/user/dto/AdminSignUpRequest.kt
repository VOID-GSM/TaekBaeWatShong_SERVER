package com.example.taekbaewatshongserver.domain.user.dto

data class AdminSignUpRequest(
    val email: String,
    val password: String,
    val name: String,
)
