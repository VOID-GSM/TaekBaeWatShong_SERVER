package com.example.taekbaewatshongserver.global.security.oauth

import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse

class DelegatingAuthorizationCodeTokenResponseClient(
    private val defaultClient: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        DefaultAuthorizationCodeTokenResponseClient(),
    private val dataGsmClient: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        DataGsmAuthorizationCodeTokenResponseClient(),
) : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    override fun getTokenResponse(authorizationGrantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse {
        val client = if (authorizationGrantRequest.clientRegistration.registrationId == AuthController.PROVIDER_DATAGSM) {
            dataGsmClient
        } else {
            defaultClient
        }
        return client.getTokenResponse(authorizationGrantRequest)
    }
}
