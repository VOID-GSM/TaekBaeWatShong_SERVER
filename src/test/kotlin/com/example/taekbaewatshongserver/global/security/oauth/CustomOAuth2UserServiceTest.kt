package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import kotlin.test.assertFailsWith

class CustomOAuth2UserServiceTest {

    private val service = CustomOAuth2UserService(
        userRepository = mock(UserRepository::class.java),
        adminEmails = emptyList(),
    )

    @Test
    fun `status가 ACTIVE가 아닌 DataGSM 계정은 로그인이 거부된다`() {
        val userInfo = DataGsmOAuth2UserInfo(
            mapOf(
                "id" to "1",
                "email" to "student@gsm.hs.kr",
                "status" to "SUSPENDED",
                "student" to mapOf("name" to "홍길동"),
            ),
        )

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.requireActiveAccount(userInfo)
        }
        assert(exception.message?.contains("SUSPENDED") == true)
    }

    @Test
    fun `status가 ACTIVE인 DataGSM 계정은 통과한다`() {
        val userInfo = DataGsmOAuth2UserInfo(
            mapOf(
                "id" to "1",
                "email" to "student@gsm.hs.kr",
                "status" to "ACTIVE",
                "student" to mapOf("name" to "홍길동"),
            ),
        )

        assertDoesNotThrow { service.requireActiveAccount(userInfo) }
    }

    @Test
    fun `구글 계정은 status 검증 대상이 아니다`() {
        val userInfo = GoogleOAuth2UserInfo(
            mapOf(
                "sub" to "1",
                "email" to "user@gmail.com",
                "name" to "홍길동",
            ),
        )

        assertDoesNotThrow { service.requireActiveAccount(userInfo) }
    }
}
