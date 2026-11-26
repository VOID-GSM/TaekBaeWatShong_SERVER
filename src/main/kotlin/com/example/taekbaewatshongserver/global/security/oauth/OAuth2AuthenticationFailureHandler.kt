package com.example.taekbaewatshongserver.global.security.oauth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.oauth2.redirect-uri}") private val clientRedirectUri: String,
    @Value("\${app.oauth2.admin-redirect-uri}") private val adminRedirectUri: String,
) : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val session = request.getSession(false)
        val loginType = session?.getAttribute(AuthController.LOGIN_TYPE_SESSION_KEY) as String?
        val redirectUri = if (loginType == AuthController.LoginType.ADMIN.name) adminRedirectUri else clientRedirectUri

        // 남은 값이 다음 로그인 시도에 섞이지 않도록, 리다이렉트 주소를 정한 뒤 모두 지운다.
        session?.apply {
            removeAttribute(AuthController.LOGIN_TYPE_SESSION_KEY)
            removeAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY)
            removeAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY)
        }

        val targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("error", exception.localizedMessage)
            .build()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
