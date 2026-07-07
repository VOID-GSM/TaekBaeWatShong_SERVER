package com.example.taekbaewatshongserver.domain.user.dto

import com.example.taekbaewatshongserver.domain.user.entity.User

data class UserResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: String,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role.name,
            )
    }
}
