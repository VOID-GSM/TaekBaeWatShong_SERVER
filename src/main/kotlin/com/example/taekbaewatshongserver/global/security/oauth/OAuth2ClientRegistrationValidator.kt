package com.example.taekbaewatshongserver.global.security.oauth

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties
import org.springframework.stereotype.Component

/**
 * OAuth2 클라이언트 설정에 환경 변수가 주입되지 않은 채(`${...}` 그대로) 남아 있으면 기동을 막는다.
 * 그대로 두면 서버는 정상 기동되지만 로그인 시점에 provider가 invalid_client(401)로 거절한다.
 */
@Component
class OAuth2ClientRegistrationValidator(
    private val properties: OAuth2ClientProperties,
) {
    @PostConstruct
    fun validate() {
        val problems = properties.registration.flatMap { (registrationId, registration) ->
            listOfNotNull(
                problemOf(registrationId, "client-id", registration.clientId),
                problemOf(registrationId, "client-secret", registration.clientSecret),
            )
        }
        check(problems.isEmpty()) {
            "OAuth2 클라이언트 설정에 환경 변수가 주입되지 않았습니다. 배포 환경 변수를 확인하세요: ${problems.joinToString()}"
        }
    }

    private fun problemOf(registrationId: String, key: String, value: String?): String? =
        if (value.isNullOrBlank() || value.contains("\${")) "$registrationId.$key=$value" else null
}
