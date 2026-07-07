package com.example.taekbaewatshongserver.domain.user

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController {

    @GetMapping("/me")
    fun me(authentication: Authentication): UserResponse {
        val user = authentication.principal as User
        return UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
    }
}

data class UserResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: String,
)