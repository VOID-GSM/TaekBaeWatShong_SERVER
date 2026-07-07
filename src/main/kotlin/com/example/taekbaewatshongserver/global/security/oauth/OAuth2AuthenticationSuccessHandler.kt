package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val oneTimeAuthCodeStore: OneTimeAuthCodeStore,
    @Value("\${app.oauth2.redirect-uri}") private val clientRedirectUri: String,
    @Value("\${app.oauth2.admin-redirect-uri}") private val adminRedirectUri: String,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as UserPrincipal
        val token = jwtTokenProvider.createToken(principal.user.id!!, principal.user.email)
        val code = oneTimeAuthCodeStore.issue(token)
        val redirectUri = if (principal.user.role == Role.ADMIN) adminRedirectUri else clientRedirectUri

        val targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("code", code)
            .build()
            .toUriString()

        clearAuthenticationAttributes(request)
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}