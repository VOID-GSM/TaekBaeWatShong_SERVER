package com.example.taekbaewatshongserver.global.security.oauth

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.web.client.RestTemplate

class DataGsmAuthorizationCodeTokenResponseClient : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private val restTemplate = RestTemplate(
        listOf(
            OAuth2AccessTokenResponseHttpMessageConverter(),
            MappingJackson2HttpMessageConverter(),
        ),
    ).apply {
        errorHandler = OAuth2ErrorResponseErrorHandler()
    }

    override fun getTokenResponse(authorizationGrantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse {
        val clientRegistration = authorizationGrantRequest.clientRegistration
        val body = mapOf(
            "grant_type" to "authorization_code",
            "code" to authorizationGrantRequest.authorizationExchange.authorizationResponse.code,
            "client_id" to clientRegistration.clientId,
            "client_secret" to clientRegistration.clientSecret,
            "redirect_uri" to authorizationGrantRequest.authorizationExchange.authorizationRequest.redirectUri,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val response = restTemplate.exchange(
            clientRegistration.providerDetails.tokenUri,
            HttpMethod.POST,
            HttpEntity(body, headers),
            OAuth2AccessTokenResponse::class.java,
        )

        return response.body ?: throw OAuth2AuthenticationException(
            OAuth2Error("invalid_token_response"),
            "DataGSM 토큰 응답을 파싱하지 못했습니다",
        )
    }
}
