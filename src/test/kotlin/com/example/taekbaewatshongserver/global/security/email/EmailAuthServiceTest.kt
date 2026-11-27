package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.admin.service.AdminInviteCodeService
import com.example.taekbaewatshongserver.domain.auth.service.RefreshTokenService
import com.example.taekbaewatshongserver.domain.user.dto.AdminSignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmailAuthServiceTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java)
    private val refreshTokenService: RefreshTokenService = mock(RefreshTokenService::class.java)
    private val adminInviteCodeService: AdminInviteCodeService = mock(AdminInviteCodeService::class.java)
    private val service = EmailAuthService(userRepository, passwordEncoder, refreshTokenService, adminInviteCodeService)

    private val code = "K7Q2-M9XA"

    private fun localUser(id: Long, email: String, role: Role) = User(id = id, email = email, name = "사용자", provider = AuthProvider.LOCAL, password = "hashed", role = role)

    // Mockito 매처는 null을 돌려주므로, Kotlin의 non-null 인자 검사에 걸리지 않게 실제 값을 대신 돌려준다.
    private fun anyUser(): User {
        ArgumentMatchers.any(User::class.java)
        return localUser(0L, "placeholder@gsm.hs.kr", Role.STUDENT)
    }

    private fun eqString(value: String): String {
        ArgumentMatchers.eq(value)
        return value
    }

    private fun sameUser(user: User): User {
        ArgumentMatchers.same(user)
        return user
    }

    private fun anyCode(): String {
        ArgumentMatchers.anyString()
        return code
    }

    private fun assertStatus(status: HttpStatus, block: () -> Unit) {
        val exception = assertFailsWith<ResponseStatusException> { block() }
        assertEquals(status, exception.statusCode)
    }

    private fun givenIssued(user: User) {
        `when`(refreshTokenService.issueOnLogin(sameUser(user))).thenReturn(TokenResponse(token = "token", refreshToken = "refresh"))
    }

    private fun givenPassword(matches: Boolean) {
        `when`(passwordEncoder.matches("password1234", "hashed")).thenReturn(matches)
    }

    @Test
    fun `기존 ADMIN은 초대 코드가 함께 와도 소비하지 않고 가입 요청을 통과한다`() {
        val admin = localUser(10L, "admin@gsm.hs.kr", Role.ADMIN)
        `when`(userRepository.findByEmail(admin.email)).thenReturn(admin)
        givenPassword(matches = true)
        givenIssued(admin)

        val response = service.adminSignUp(AdminSignUpRequest(admin.email, "password1234", "관리자", inviteCode = code))

        assertEquals("token", response.token)
        assertEquals("refresh", response.refreshToken)
        verify(refreshTokenService).issueOnLogin(sameUser(admin))
        verify(adminInviteCodeService, never()).consume(anyCode(), anyUser())
    }

    @Test
    fun `기존 STUDENT 계정은 유효한 초대 코드로 ADMIN 승격된다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        val promoted = localUser(1L, student.email, Role.ADMIN)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        givenPassword(matches = true)
        `when`(adminInviteCodeService.consume(eqString(code), sameUser(student))).thenReturn(promoted)
        givenIssued(promoted)

        val response = service.adminSignUp(AdminSignUpRequest(student.email, "password1234", "학생", inviteCode = code))

        assertEquals("token", response.token)
        assertEquals("refresh", response.refreshToken)
        verify(adminInviteCodeService).consume(eqString(code), sameUser(student))
        verify(refreshTokenService).issueOnLogin(sameUser(promoted))
    }

    @Test
    fun `새 계정은 유효한 초대 코드로 LOCAL ADMIN 계정이 만들어진다`() {
        val email = "new-admin@gsm.hs.kr"
        `when`(userRepository.findByEmail(email)).thenReturn(null)
        `when`(passwordEncoder.encode("password1234")).thenReturn("hashed")
        val passed = mutableListOf<User>()
        `when`(adminInviteCodeService.consume(eqString(code), anyUser())).thenAnswer {
            val user = it.getArgument<User>(1).also(passed::add)
            User(id = 5L, email = user.email, name = user.name, provider = user.provider, password = user.password, role = user.role)
        }
        `when`(refreshTokenService.issueOnLogin(anyUser())).thenAnswer {
            TokenResponse(token = "token-${it.getArgument<User>(0).id}", refreshToken = "refresh")
        }

        val response = service.adminSignUp(AdminSignUpRequest(email, "password1234", "새관리자", inviteCode = code))

        assertEquals("token-5", response.token)
        assertEquals("refresh", response.refreshToken)
        val created = passed.single()
        assertNull(created.id)
        assertEquals(email, created.email)
        assertEquals("새관리자", created.name)
        assertEquals(AuthProvider.LOCAL, created.provider)
        assertEquals("hashed", created.password)
        assertEquals(Role.ADMIN, created.role)
    }

    @Test
    fun `만료되었거나 인원이 다 찬 초대 코드는 AdminInviteCodeException Invalid로 거절된다`() {
        val email = "new-admin@gsm.hs.kr"
        `when`(userRepository.findByEmail(email)).thenReturn(null)
        `when`(passwordEncoder.encode("password1234")).thenReturn("hashed")
        `when`(adminInviteCodeService.consume(eqString(code), anyUser())).thenThrow(AdminInviteCodeException.Invalid())

        assertFailsWith<AdminInviteCodeException.Invalid> {
            service.adminSignUp(AdminSignUpRequest(email, "password1234", "새관리자", inviteCode = code))
        }
    }

    @Test
    fun `새 계정 저장이 이메일 중복으로 실패하면 409로 바꾼다`() {
        val email = "new-admin@gsm.hs.kr"
        `when`(userRepository.findByEmail(email)).thenReturn(null)
        `when`(passwordEncoder.encode("password1234")).thenReturn("hashed")
        `when`(adminInviteCodeService.consume(eqString(code), anyUser())).thenThrow(DataIntegrityViolationException("duplicate"))

        assertStatus(HttpStatus.CONFLICT) {
            service.adminSignUp(AdminSignUpRequest(email, "password1234", "새관리자", inviteCode = code))
        }
    }

    @Test
    fun `초대 코드가 없거나 공백이면 ADMIN이 아닌 계정은 403으로 거절된다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        givenPassword(matches = true)

        assertStatus(HttpStatus.FORBIDDEN) { service.adminSignUp(AdminSignUpRequest(student.email, "password1234", "학생")) }
        assertStatus(HttpStatus.FORBIDDEN) { service.adminSignUp(AdminSignUpRequest(student.email, "password1234", "학생", inviteCode = "   ")) }

        `when`(userRepository.findByEmail("new@gsm.hs.kr")).thenReturn(null)
        assertStatus(HttpStatus.FORBIDDEN) { service.adminSignUp(AdminSignUpRequest("new@gsm.hs.kr", "password1234", "새사람")) }

        assertEquals(Role.STUDENT, student.role)
        verify(adminInviteCodeService, never()).consume(anyCode(), anyUser())
        verify(userRepository, never()).save(ArgumentMatchers.any(User::class.java))
    }

    @Test
    fun `이메일 경로에는 부트스트랩 이메일 예외가 없다`() {
        val email = "teamvoid0107@gmail.com"
        `when`(userRepository.findByEmail(email)).thenReturn(null)

        assertStatus(HttpStatus.FORBIDDEN) { service.adminSignUp(AdminSignUpRequest(email, "password1234", "VOID")) }
        verify(userRepository, never()).save(ArgumentMatchers.any(User::class.java))
    }

    @Test
    fun `비밀번호가 틀리거나 소셜 가입 계정이면 초대 코드를 소비하기 전에 거절된다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        givenPassword(matches = false)
        assertStatus(HttpStatus.UNAUTHORIZED) {
            service.adminSignUp(AdminSignUpRequest(student.email, "password1234", "학생", inviteCode = code))
        }

        val social = User(id = 2L, email = "social@gsm.hs.kr", name = "소셜", provider = AuthProvider.GOOGLE, role = Role.STUDENT)
        `when`(userRepository.findByEmail(social.email)).thenReturn(social)
        assertStatus(HttpStatus.CONFLICT) {
            service.adminSignUp(AdminSignUpRequest(social.email, "password1234", "소셜", inviteCode = code))
        }

        verify(adminInviteCodeService, never()).consume(anyCode(), anyUser())
    }

    @Test
    fun `관리자 로그인은 role이 ADMIN일 때만 통과한다`() {
        val admin = localUser(10L, "admin@gsm.hs.kr", Role.ADMIN)
        `when`(userRepository.findByEmail(admin.email)).thenReturn(admin)
        givenPassword(matches = true)
        givenIssued(admin)

        val response = service.adminLogin(EmailLoginRequest(admin.email, "password1234"))

        assertEquals("token", response.token)
        assertEquals("refresh", response.refreshToken)
        verify(refreshTokenService).issueOnLogin(sameUser(admin))
    }

    @Test
    fun `관리자 로그인에서 ADMIN이 아니면 부트스트랩 이메일이어도 승격 없이 403이다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        val bootstrap = localUser(2L, "teamvoid0107@gmail.com", Role.STUDENT)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        `when`(userRepository.findByEmail(bootstrap.email)).thenReturn(bootstrap)
        givenPassword(matches = true)

        assertStatus(HttpStatus.FORBIDDEN) { service.adminLogin(EmailLoginRequest(student.email, "password1234")) }
        assertStatus(HttpStatus.FORBIDDEN) { service.adminLogin(EmailLoginRequest(bootstrap.email, "password1234")) }

        assertEquals(Role.STUDENT, student.role)
        assertEquals(Role.STUDENT, bootstrap.role)
        verify(refreshTokenService, never()).issueOnLogin(anyUser())
    }

    @Test
    fun `일반 로그인은 refresh token을 함께 발급한다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        givenPassword(matches = true)
        givenIssued(student)

        val response = service.login(EmailLoginRequest(student.email, "password1234"))

        assertEquals("token", response.token)
        assertEquals("refresh", response.refreshToken)
        verify(refreshTokenService).issueOnLogin(sameUser(student))
    }

    @Test
    fun `비밀번호가 틀리면 refresh token을 발급하지 않는다`() {
        val student = localUser(1L, "student@gsm.hs.kr", Role.STUDENT)
        `when`(userRepository.findByEmail(student.email)).thenReturn(student)
        givenPassword(matches = false)

        assertStatus(HttpStatus.UNAUTHORIZED) { service.login(EmailLoginRequest(student.email, "password1234")) }
        verify(refreshTokenService, never()).issueOnLogin(anyUser())
    }

    @Test
    fun `회원가입은 저장된 사용자로 refresh token을 발급한다`() {
        val email = "new@gsm.hs.kr"
        `when`(userRepository.existsByEmail(email)).thenReturn(false)
        `when`(passwordEncoder.encode("password1234")).thenReturn("hashed")
        val saved = localUser(7L, email, Role.STUDENT)
        `when`(userRepository.save(anyUser())).thenReturn(saved)
        givenIssued(saved)

        val response = service.signUp(SignUpRequest(email, "password1234", "새학생", Role.STUDENT, studentNumber = "1101"))

        assertEquals("token", response.token)
        assertEquals("refresh", response.refreshToken)
        verify(refreshTokenService).issueOnLogin(sameUser(saved))
    }

    @Test
    fun `로그인 두 경로는 refresh token 저장을 위해 쓰기 트랜잭션으로 열린다`() {
        listOf("login", "adminLogin").forEach { name ->
            val transactional = EmailAuthService::class.java.getMethod(name, EmailLoginRequest::class.java)
                .getAnnotation(Transactional::class.java)
            assertNotNull(transactional, name)
            assertFalse(transactional.readOnly, name)
        }
    }
}
