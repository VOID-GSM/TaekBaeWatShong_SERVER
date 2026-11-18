package com.example.taekbaewatshongserver.global.security.jwt

import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional
import kotlin.test.assertNull
import kotlin.test.assertSame

class JwtAuthenticationFilterTest {

    private val provider = JwtTokenProvider(SECRET, 60_000L)
    private val expiredToken = JwtTokenProvider(SECRET, -60_000L).createToken(USER_ID, "user@gsm.hs.kr")
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val filter = JwtAuthenticationFilter(provider, userRepository)
    private val user = User(
        id = USER_ID,
        email = "user@gsm.hs.kr",
        name = "이학생",
        provider = AuthProvider.DATAGSM,
        role = Role.STUDENT,
    )

    init {
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
    }

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    // Spring Security 매처는 servletPath로 경로를 본다. DispatcherServlet이 "/"에 매핑돼 있으면 servletPath가 전체 경로다.
    private fun request(method: String, path: String, token: String) = MockHttpServletRequest(method, path).apply {
        servletPath = path
        addHeader("Authorization", "Bearer $token")
    }

    private fun authenticatedPrincipal(request: MockHttpServletRequest): Any? {
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
        return SecurityContextHolder.getContext().authentication?.principal
    }

    @Test
    fun `유효한 토큰은 어느 요청에서든 인증된다`() {
        val token = provider.createToken(USER_ID, "user@gsm.hs.kr")

        assertSame(user, authenticatedPrincipal(request("GET", "/notification", token)))
    }

    @Test
    fun `만료된 토큰이어도 디바이스 토큰 삭제 요청은 인증된다`() {
        assertSame(user, authenticatedPrincipal(request("DELETE", "/notification/token", expiredToken)))
    }

    @Test
    fun `만료된 토큰으로 다른 요청을 보내면 인증되지 않는다`() {
        assertNull(authenticatedPrincipal(request("GET", "/notification", expiredToken)))
    }

    @Test
    fun `만료된 토큰으로 같은 경로에 다른 메서드를 보내면 인증되지 않는다`() {
        assertNull(authenticatedPrincipal(request("POST", "/notification/token", expiredToken)))
    }

    @Test
    fun `다른 키로 서명한 만료 토큰은 디바이스 토큰 삭제 요청에서도 인증되지 않는다`() {
        val forged = JwtTokenProvider(OTHER_SECRET, -60_000L).createToken(USER_ID, "user@gsm.hs.kr")

        assertNull(authenticatedPrincipal(request("DELETE", "/notification/token", forged)))
    }

    companion object {
        private const val USER_ID = 7L
        private const val SECRET = "test-secret-key-that-is-at-least-32-bytes!!"
        private const val OTHER_SECRET = "another-secret-key-that-is-at-least-32-bytes"
    }
}
