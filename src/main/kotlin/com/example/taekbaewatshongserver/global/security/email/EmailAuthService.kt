package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.user.dto.AdminSignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class EmailAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("#{'\${app.admin.emails:}'.split(',')}") adminEmails: List<String>,
) {

    private val adminEmails: Set<String> = adminEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun signUp(request: SignUpRequest): TokenResponse {
        if (request.role != Role.STUDENT && request.role != Role.TEACHER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role은 STUDENT 또는 TEACHER만 선택할 수 있습니다")
        }
        return createLocalUser(email = request.email, password = request.password, name = request.name, role = request.role)
    }

    fun adminSignUp(request: AdminSignUpRequest): TokenResponse {
        if (request.email.lowercase() !in adminEmails) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정이 아닙니다")
        }
        return createLocalUser(email = request.email, password = request.password, name = request.name, role = Role.ADMIN)
    }

    fun login(request: EmailLoginRequest): TokenResponse =
        authenticate(request.email, request.password, requiredRole = null)

    fun adminLogin(request: EmailLoginRequest): TokenResponse =
        authenticate(request.email, request.password, requiredRole = Role.ADMIN)

    private fun createLocalUser(email: String, password: String, name: String, role: Role): TokenResponse {
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다")
        }

        val user = try {
            userRepository.save(
                User(
                    email = email,
                    name = name,
                    provider = AuthProvider.LOCAL,
                    password = passwordEncoder.encode(password),
                    role = role,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다")
        }

        return TokenResponse(jwtTokenProvider.createToken(requireNotNull(user.id), user.email))
    }

    private fun authenticate(email: String, password: String, requiredRole: Role?): TokenResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")

        if (user.provider != AuthProvider.LOCAL || user.password == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 로그인으로 가입된 계정입니다")
        }

        if (!passwordEncoder.matches(password, user.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        if (requiredRole != null && user.role != requiredRole) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정이 아닙니다")
        }

        return TokenResponse(jwtTokenProvider.createToken(requireNotNull(user.id), user.email))
    }
}
