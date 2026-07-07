package com.example.taekbaewatshongserver.global.security.oauth

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val id: String
        get() = attributes["sub"] as String

    override val name: String
        get() = attributes["name"] as String

    override val email: String
        get() = attributes["email"] as String
}