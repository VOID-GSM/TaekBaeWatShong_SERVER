package com.example.taekbaewatshongserver.global.security.oauth

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val id: String
        get() = requiredAttribute("sub")

    override val name: String
        get() = attributes["name"]?.toString() ?: "Unknown"

    override val email: String
        get() = requiredAttribute("email")

    private fun requiredAttribute(key: String): String =
        attributes[key]?.toString()
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("missing_attribute"),
                "구글 계정에서 '$key' 정보를 가져오지 못했습니다",
            )
}
