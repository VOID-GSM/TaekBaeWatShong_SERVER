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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class EmailAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("#{'\${app.admin.emails:}'.split(',')}") adminEmails: List<String>,
) {

    private val adminEmails: Set<String> = adminEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    @Transactional
    fun signUp(request: SignUpRequest): TokenResponse {
        if (request.role != Role.STUDENT && request.role != Role.TEACHER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role은 STUDENT 또는 TEACHER만 선택할 수 있습니다")
        }
        return createLocalUser(
            email = request.email,
            password = request.password,
            name = request.name,
            role = request.role,
            studentNumber = request.studentNumber.takeIf { request.role == Role.STUDENT },
        )
    }

    @Transactional
    fun adminSignUp(request: AdminSignUpRequest): TokenResponse {
        if (request.email.lowercase() !in adminEmails) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "\uAD00\uB9AC\uC790 \uACC4\uC815\uC774 \uC544\uB2D9\uB2C8\uB2E4")
        }

        val existing = userRepository.findByEmail(request.email)
        if (existing != null) {
            if (existing.provider != AuthProvider.LOCAL || existing.password == null) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "\uC18C\uC15C \uB85C\uADF8\uC778\uC73C\uB85C \uAC00\uC785\uB41C \uACC4\uC815\uC740 \uD574\uB2F9 \uC18C\uC15C \uB85C\uADF8\uC778\uC73C\uB85C \uAD00\uB9AC\uC790 \uB85C\uADF8\uC778\uC744 \uD574\uC8FC\uC138\uC694")
            }
            if (!passwordEncoder.matches(request.password, existing.password)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "\uC774\uBA54\uC77C \uB610\uB294 \uBE44\uBC00\uBC88\uD638\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4")
            }
            existing.role = Role.ADMIN
            return TokenResponse(jwtTokenProvider.createToken(requireNotNull(existing.id), existing.email))
        }

        return createLocalUser(email = request.email, password = request.password, name = request.name, role = Role.ADMIN)
    }

    fun login(request: EmailLoginRequest): TokenResponse = authenticate(request.email, request.password, requiredRole = null)

    @Transactional
    fun adminLogin(request: EmailLoginRequest): TokenResponse = authenticate(request.email, request.password, requiredRole = Role.ADMIN)

    private fun createLocalUser(
        email: String,
        password: String,
        name: String,
        role: Role,
        studentNumber: String? = null,
    ): TokenResponse {
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
                    studentNumber = studentNumber,
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
            val providerName = when (user.provider) {
                AuthProvider.GOOGLE -> "Google"
                AuthProvider.DATAGSM -> "DataGSM"
                AuthProvider.LOCAL -> "이메일"
            }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "$providerName 로그인으로 가입된 계정입니다")
        }
        if (!passwordEncoder.matches(password, user.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        if (requiredRole == Role.ADMIN && user.role != Role.ADMIN) {
            if (email.lowercase() !in adminEmails) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "\uAD00\uB9AC\uC790 \uACC4\uC815\uC774 \uC544\uB2D9\uB2C8\uB2E4")
            }
            user.role = Role.ADMIN
        }
        return TokenResponse(jwtTokenProvider.createToken(requireNotNull(user.id), user.email))
    }
}
