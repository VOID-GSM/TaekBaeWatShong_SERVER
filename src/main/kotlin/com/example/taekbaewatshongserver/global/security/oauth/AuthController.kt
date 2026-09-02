package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.Role
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class AuthController(
    private val oneTimeAuthCodeStore: OneTimeAuthCodeStore,
) {

    @GetMapping("/auth/login")
    fun login(@RequestParam role: Role, request: HttpServletRequest, response: HttpServletResponse) {
        if (role != Role.STUDENT && role != Role.TEACHER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role은 STUDENT 또는 TEACHER만 선택할 수 있습니다")
        }
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.CLIENT.name)
        request.session.setAttribute(SIGNUP_ROLE_SESSION_KEY, role.name)
        response.sendRedirect("/oauth2/authorization/google")
    }

    @GetMapping("/auth/admin/login")
    fun adminLogin(request: HttpServletRequest, response: HttpServletResponse) {
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.ADMIN.name)
        response.sendRedirect("/oauth2/authorization/google")
    }

    @GetMapping("/auth/token")
    fun exchangeToken(@RequestParam code: String): TokenResponse {
        val token = oneTimeAuthCodeStore.consume(code)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 code입니다")
        return TokenResponse(token)
    }

    enum class LoginType {
        CLIENT,
        ADMIN,
    }

    companion object {
        const val SIGNUP_ROLE_SESSION_KEY = "signup_role"
        const val LOGIN_TYPE_SESSION_KEY = "login_type"
    }
}
