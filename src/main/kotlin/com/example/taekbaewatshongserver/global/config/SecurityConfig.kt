package com.example.taekbaewatshongserver.global.config

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtAuthenticationFilter
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import com.example.taekbaewatshongserver.global.security.oauth.CustomOAuth2UserService
import com.example.taekbaewatshongserver.global.security.oauth.OAuth2AuthenticationFailureHandler
import com.example.taekbaewatshongserver.global.security.oauth.OAuth2AuthenticationSuccessHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    @Value("#{'\${app.cors.allowed-origins}'.split(',')}") private val allowedOrigins: List<String>,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/", "/auth/login", "/auth/admin/login", "/oauth2/**", "/login/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { it.userService(customOAuth2UserService) }
                oauth2.successHandler(oAuth2AuthenticationSuccessHandler)
                oauth2.failureHandler(oAuth2AuthenticationFailureHandler)
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}