package com.example.taekbaewatshongserver.domain.user.controller

import com.example.taekbaewatshongserver.domain.user.dto.UserResponse
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: User): UserResponse =
        UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
}