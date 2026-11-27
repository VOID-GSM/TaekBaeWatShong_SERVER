package com.example.taekbaewatshongserver.global.security.oauth

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OAuth2AuthenticationFailureHandlerTest {

    private val handler = OAuth2AuthenticationFailureHandler(
        clientRedirectUri = "taekbaewatshong://oauth2/redirect",
        adminRedirectUri = "taekbaewatshong-admin://oauth2/redirect",
    )

    @Test
    fun `한글 오류 문구는 퍼센트 인코딩되어 관리자 앱 주소로 리다이렉트된다`() {
        val request = adminRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(request, response, failure("not_admin", "관리자 계정이 아닙니다"))

        val location = assertNotNull(response.redirectedUrl)
        assertEquals(
            "taekbaewatshong-admin://oauth2/redirect?error=%EA%B4%80%EB%A6%AC%EC%9E%90%20%EA%B3%84%EC%A0%95%EC%9D%B4%20%EC%95%84%EB%8B%99%EB%8B%88%EB%8B%A4",
            location,
        )
        assertEquals("관리자 계정이 아닙니다", decodedError(location))
    }

    @Test
    fun `중괄호와 따옴표가 든 문구도 모두 인코딩되어 유효한 URI가 된다`() {
        val message = "[invalid_token_response] An error occurred: {\"error\":\"invalid_client\"}"
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(adminRequest(), response, failure("invalid_token_response", message))

        val location = assertNotNull(response.redirectedUrl)
        assertFalse(location.any { it == '{' || it == '}' || it == '"' || it == ' ' || it.code > 0x7F }, location)
        URI(location)
        assertEquals(message, decodedError(location))
    }

    @Test
    fun `앰퍼샌드와 등호는 다른 쿼리 파라미터로 쪼개지지 않는다`() {
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(adminRequest(), response, failure("x", "a&b=c"))

        val location = assertNotNull(response.redirectedUrl)
        assertEquals("a&b=c", decodedError(location))
    }

    @Test
    fun `login_type이 ADMIN이 아니면 사용자 앱 주소로 리다이렉트된다`() {
        val request = MockHttpServletRequest()
        request.session!!.setAttribute(AuthController.LOGIN_TYPE_SESSION_KEY, AuthController.LoginType.CLIENT.name)
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(request, response, failure("role_required", "역할이 필요합니다"))

        assert(response.redirectedUrl!!.startsWith("taekbaewatshong://oauth2/redirect?error="))
    }

    @Test
    fun `실패하면 세션의 로그인 키 세 개를 모두 지운다`() {
        val request = adminRequest()
        request.session!!.setAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY, "STUDENT")
        request.session!!.setAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY, "K7Q2M9XA")

        handler.onAuthenticationFailure(request, MockHttpServletResponse(), failure("not_admin", "관리자 계정이 아닙니다"))

        assertNull(request.session!!.getAttribute(AuthController.LOGIN_TYPE_SESSION_KEY))
        assertNull(request.session!!.getAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY))
        assertNull(request.session!!.getAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY))
    }

    @Test
    fun `세션이 없으면 새로 만들지 않고 사용자 앱 주소로 리다이렉트된다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(request, response, failure("x", "실패"))

        assertNull(request.getSession(false))
        assert(response.redirectedUrl!!.startsWith("taekbaewatshong://oauth2/redirect?error="))
    }

    private fun adminRequest() = MockHttpServletRequest().apply {
        session!!.setAttribute(AuthController.LOGIN_TYPE_SESSION_KEY, AuthController.LoginType.ADMIN.name)
    }

    private fun failure(code: String, message: String) = OAuth2AuthenticationException(OAuth2Error(code), message)

    private fun decodedError(location: String): String? = UriComponentsBuilder.fromUriString(location).build(true).queryParams.getFirst("error")
        ?.let { java.net.URLDecoder.decode(it.replace("+", "%2B"), Charsets.UTF_8) }
}
