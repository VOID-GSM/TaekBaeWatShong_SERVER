package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.user.dto.AdminSignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@RestController
@RequestMapping("/auth/email")
class EmailAuthController(
    private val emailAuthService: EmailAuthService,
) {

    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): TokenResponse {
        validateEmail(request.email)
        validatePassword(request.password)
        if (request.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 비어있을 수 없습니다")
        }
        return emailAuthService.signUp(request)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: EmailLoginRequest): TokenResponse {
        validateEmail(request.email)
        return emailAuthService.login(request)
    }

    @PostMapping("/admin/signup")
    fun adminSignUp(@RequestBody request: AdminSignUpRequest): TokenResponse {
        validateEmail(request.email)
        validatePassword(request.password)
        if (request.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 비어있을 수 없습니다")
        }
        return emailAuthService.adminSignUp(request)
    }

    @PostMapping("/admin/login")
    fun adminLogin(@RequestBody request: EmailLoginRequest): TokenResponse {
        validateEmail(request.email)
        return emailAuthService.adminLogin(request)
    }

    private fun validateEmail(email: String) {
        if (!EMAIL_REGEX.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 이메일 형식이 아닙니다")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다")
        }
    }
}
