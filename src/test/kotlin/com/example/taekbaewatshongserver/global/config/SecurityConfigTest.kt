package com.example.taekbaewatshongserver.global.config

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import com.example.taekbaewatshongserver.global.security.oauth.CustomOAuth2UserService
import com.example.taekbaewatshongserver.global.security.oauth.OAuth2AuthenticationFailureHandler
import com.example.taekbaewatshongserver.global.security.oauth.OAuth2AuthenticationSuccessHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.options
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/**
 * 실제 [SecurityConfig] 필터 체인만 띄워 접근 규칙을 확인한다. 컨트롤러는 경로만 같은 대역이라 서비스·DB가 필요 없다.
 */
@SpringJUnitWebConfig(SecurityConfigTest.TestConfig::class)
class SecurityConfigTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val expiredToken = JwtTokenProvider(SECRET, -60_000L).createToken(USER_ID, "user@gsm.hs.kr")
    private val forgedToken = JwtTokenProvider("another-secret-another-secret-another-secret", 60_000L).createToken(USER_ID, "user@gsm.hs.kr")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @Test
    fun `refresh는 토큰 없이 호출할 수 있다`() {
        mockMvc.post("/auth/refresh").andExpect { status { isOk() } }
    }

    @Test
    fun `refresh에 만료된 Bearer 헤더가 실려 있어도 필터가 막지 않는다`() {
        mockMvc.post("/auth/refresh") { header("Authorization", "Bearer $expiredToken") }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `refresh에 서명이 틀린 Bearer 헤더가 실려 있어도 필터가 막지 않는다`() {
        mockMvc.post("/auth/refresh") { header("Authorization", "Bearer $forgedToken") }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `logout은 Spring 기본 LogoutFilter가 아니라 컨트롤러에 도달한다`() {
        mockMvc.post("/auth/logout").andExpect {
            status { isNoContent() }
            header { doesNotExist("Location") }
        }
    }

    @Test
    fun `logout에 만료된 Bearer 헤더가 실려 있어도 컨트롤러에 도달한다`() {
        mockMvc.post("/auth/logout") { header("Authorization", "Bearer $expiredToken") }
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `허용 목록에 없는 경로는 만료 토큰이면 지금처럼 로그인 페이지로 302 리다이렉트된다`() {
        mockMvc.post("/protected") { header("Authorization", "Bearer $expiredToken") }
            .andExpect {
                status { isFound() }
                redirectedUrlPattern("**/login")
            }
    }

    @Test
    fun `허용된 출처의 refresh·logout 사전 요청은 CORS를 통과한다`() {
        listOf("/auth/refresh", "/auth/logout").forEach { path ->
            mockMvc.options(path) {
                header("Origin", ALLOWED_ORIGIN)
                header("Access-Control-Request-Method", "POST")
                header("Access-Control-Request-Headers", "content-type")
            }.andExpect {
                status { isOk() }
                header { string("Access-Control-Allow-Origin", ALLOWED_ORIGIN) }
            }
        }
    }

    @Test
    fun `허용되지 않은 출처의 refresh 사전 요청은 거부된다`() {
        mockMvc.options("/auth/refresh") {
            header("Origin", "https://evil.example.com")
            header("Access-Control-Request-Method", "POST")
        }.andExpect { status { isForbidden() } }
    }

    @RestController
    class StubController {
        @PostMapping("/auth/refresh")
        fun refresh(): ResponseEntity<String> = ResponseEntity.ok("refreshed")

        @PostMapping("/auth/logout")
        fun logout(): ResponseEntity<Void> = ResponseEntity.noContent().build()

        @PostMapping("/protected")
        fun protectedEndpoint(): ResponseEntity<String> = ResponseEntity.ok("protected")
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig::class, StubController::class)
    class TestConfig {
        @Bean
        fun customOAuth2UserService(): CustomOAuth2UserService = mock(CustomOAuth2UserService::class.java)

        @Bean
        fun oAuth2AuthenticationSuccessHandler(): OAuth2AuthenticationSuccessHandler = mock(OAuth2AuthenticationSuccessHandler::class.java)

        @Bean
        fun oAuth2AuthenticationFailureHandler(): OAuth2AuthenticationFailureHandler = mock(OAuth2AuthenticationFailureHandler::class.java)

        @Bean
        fun jwtTokenProvider(): JwtTokenProvider = JwtTokenProvider(SECRET, 60_000L)

        @Bean
        fun userRepository(): UserRepository = mock(UserRepository::class.java)

        @Bean
        fun corsAllowedOrigins(): CorsAllowedOrigins = CorsAllowedOrigins("$ALLOWED_ORIGIN,http://localhost:3001")

        // 운영처럼 등록이 둘이면 oauth2Login 진입점은 /login 이다.
        @Bean
        fun clientRegistrationRepository(): ClientRegistrationRepository = InMemoryClientRegistrationRepository(
            CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("id").clientSecret("secret").build(),
            CommonOAuth2Provider.GOOGLE.getBuilder("datagsm").clientId("id").clientSecret("secret").build(),
        )
    }

    companion object {
        private const val SECRET = "test-secret-test-secret-test-secret-123456"
        private const val USER_ID = 1L
        private const val ALLOWED_ORIGIN = "http://localhost:3000"
    }
}
