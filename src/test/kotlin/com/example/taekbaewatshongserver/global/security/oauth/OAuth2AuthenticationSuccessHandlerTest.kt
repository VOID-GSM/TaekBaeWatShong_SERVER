package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.auth.service.RefreshTokenService
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.web.util.UriComponentsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OAuth2AuthenticationSuccessHandlerTest {

    private val tokens = TokenResponse(token = "access-jwt", refreshToken = "q3J9vX0bA1c2D3e4F5g6H7i8J9k0L1m2N3o4P5q6R7s")

    // Kotlin의 non-null 파라미터와 Mockito 매처(any())가 충돌하지 않도록, 매처 대신 기본 Answer로 동작을 정한다.
    private val issuedFor = mutableListOf<User>()
    private val refreshTokenService: RefreshTokenService = mock(RefreshTokenService::class.java) { invocation ->
        when (invocation.method.name) {
            "issueOnLogin" -> tokens.also { issuedFor += invocation.getArgument<User>(0) }
            else -> null
        }
    }
    private val oneTimeAuthCodeStore = OneTimeAuthCodeStore()
    private val handler = OAuth2AuthenticationSuccessHandler(
        refreshTokenService = refreshTokenService,
        oneTimeAuthCodeStore = oneTimeAuthCodeStore,
        clientRedirectUri = "taekbaewatshong://oauth2/redirect",
        adminRedirectUri = "taekbaewatshong-admin://oauth2/redirect",
    )

    @Test
    fun `로그인한 사용자로 refresh token을 발급하고 code로 두 토큰을 함께 교환하게 한다`() {
        val user = user(Role.STUDENT)
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(MockHttpServletRequest(), response, authentication(user))

        assertEquals(listOf(user), issuedFor)
        val location = assertNotNull(response.redirectedUrl)
        assertTrue(location.startsWith("taekbaewatshong://oauth2/redirect?code="), location)
        assertSame(tokens, oneTimeAuthCodeStore.consume(code(location)))
    }

    @Test
    fun `관리자 계정이면 관리자 앱 주소로 code를 보낸다`() {
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(MockHttpServletRequest(), response, authentication(user(Role.ADMIN)))

        val location = assertNotNull(response.redirectedUrl)
        assertTrue(location.startsWith("taekbaewatshong-admin://oauth2/redirect?code="), location)
        assertSame(tokens, oneTimeAuthCodeStore.consume(code(location)))
    }

    @Test
    fun `리다이렉트 주소에는 토큰 원문을 싣지 않는다`() {
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(MockHttpServletRequest(), response, authentication(user(Role.TEACHER)))

        val location = assertNotNull(response.redirectedUrl)
        assertTrue(!location.contains(tokens.token) && !location.contains(tokens.refreshToken), location)
    }

    private fun user(role: Role) = User(
        id = 7L,
        email = "user@gsm.hs.kr",
        name = "이사용자",
        provider = AuthProvider.GOOGLE,
        role = role,
    )

    private fun authentication(user: User) = TestingAuthenticationToken(UserPrincipal(user), null)

    private fun code(location: String): String = assertNotNull(UriComponentsBuilder.fromUriString(location).build().queryParams.getFirst("code"))
}
