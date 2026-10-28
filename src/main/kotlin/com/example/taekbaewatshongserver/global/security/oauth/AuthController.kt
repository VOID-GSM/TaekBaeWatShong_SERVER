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
    fun login(
        @RequestParam(required = false) role: Role?,
        @RequestParam(defaultValue = PROVIDER_GOOGLE) provider: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val normalizedProvider = validateProvider(provider)
        // datagsm 계정은 objectType(STUDENT/TEACHER)으로 역할을 결정하므로 role 파라미터가 없어도 된다.
        if (normalizedProvider != PROVIDER_DATAGSM && (role != Role.STUDENT && role != Role.TEACHER)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role은 STUDENT 또는 TEACHER만 선택할 수 있습니다")
        }
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.CLIENT.name)
        if (role == Role.STUDENT || role == Role.TEACHER) {
            request.session.setAttribute(SIGNUP_ROLE_SESSION_KEY, role.name)
        }
        response.sendRedirect("/oauth2/authorization/$normalizedProvider")
    }

    @GetMapping("/auth/admin/login")
    fun adminLogin(
        @RequestParam(defaultValue = PROVIDER_GOOGLE) provider: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val normalizedProvider = validateProvider(provider)
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.ADMIN.name)
        response.sendRedirect("/oauth2/authorization/$normalizedProvider")
    }

    private fun validateProvider(provider: String): String {
        val normalized = provider.lowercase()
        if (normalized != PROVIDER_GOOGLE && normalized != PROVIDER_DATAGSM) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "provider는 google 또는 datagsm만 선택할 수 있습니다")
        }
        return normalized
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
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_DATAGSM = "datagsm"
    }
}
