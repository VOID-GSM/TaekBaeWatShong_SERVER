package com.example.taekbaewatshongserver.global.security.oauth

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

class DataGsmOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val id: String
        get() = requiredAttribute("id")

    override val email: String
        get() = requiredAttribute("email")

    override val name: String
        get() = person()["name"]?.toString() ?: "Unknown"

    // teacher 객체의 필드명이 아직 확인되지 않아, name을 못 가져온 경우 기존 이름을 덮어쓰지 않기 위한 플래그
    val hasKnownName: Boolean
        get() = person()["name"] != null

    val status: String?
        get() = attributes["status"]?.toString()

    val objectType: String?
        get() = attributes["objectType"]?.toString()

    val studentNumber: String?
        get() = person()["studentNumber"]?.toString()

    @Suppress("UNCHECKED_CAST")
    private fun person(): Map<String, Any> = (attributes["student"] ?: attributes["teacher"]) as? Map<String, Any> ?: emptyMap()

    private fun requiredAttribute(key: String): String = attributes[key]?.toString()
        ?: throw OAuth2AuthenticationException(
            OAuth2Error("missing_attribute"),
            "DataGSM 계정에서 '$key' 정보를 가져오지 못했습니다",
        )
}
