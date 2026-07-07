package com.example.taekbaewatshongserver.global.security.oauth

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController {

    @GetMapping("/auth/login")
    fun login(response: HttpServletResponse) {
        response.sendRedirect("/oauth2/authorization/google")
    }
}