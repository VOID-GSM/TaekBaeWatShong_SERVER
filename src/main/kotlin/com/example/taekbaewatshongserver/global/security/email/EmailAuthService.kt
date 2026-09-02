package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class EmailAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    fun signUp(request: SignUpRequest): TokenResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다")
        }

        val user = userRepository.save(
            User(
                email = request.email,
                name = request.name,
                provider = AuthProvider.LOCAL,
                password = passwordEncoder.encode(request.password),
                role = request.role,
            ),
        )

        return TokenResponse(jwtTokenProvider.createToken(requireNotNull(user.id), user.email))
    }

    fun login(request: EmailLoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")

        if (user.provider != AuthProvider.LOCAL || user.password == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 로그인으로 가입된 계정입니다")
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        return TokenResponse(jwtTokenProvider.createToken(requireNotNull(user.id), user.email))
    }
}
