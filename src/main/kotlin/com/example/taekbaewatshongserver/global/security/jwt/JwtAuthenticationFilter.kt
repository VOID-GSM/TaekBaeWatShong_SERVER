package com.example.taekbaewatshongserver.global.security.jwt

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        val claims = token?.let {
            if (EXPIRED_TOKEN_ALLOWED.matches(request)) {
                jwtTokenProvider.getClaimsAllowingExpired(it)
            } else {
                jwtTokenProvider.getClaims(it)
            }
        }

        if (claims != null) {
            val userId = claims.subject.toLong()
            val user = userRepository.findById(userId).orElse(null)

            if (user != null) {
                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role}")),
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }

    companion object {
        /**
         * 액세스 토큰이 만료돼도 서명만 맞으면 인증해 주는 요청.
         *
         * 리프레시 토큰이 없어 로그인 1시간 뒤에는 토큰이 항상 만료돼 있는데, 앱은 이때 401을 받고 로그아웃한다.
         * 로그아웃하면서 이 기기의 푸시 토큰을 지우지 못하면 이전 계정의 알림이 계속 가므로, 이 정리 요청만 만료를 허용한다.
         * 본인 소유 토큰만 지우는 조건은 서비스에서 그대로 지킨다. 다른 요청은 만료된 토큰을 받지 않는다.
         */
        private val EXPIRED_TOKEN_ALLOWED: RequestMatcher =
            AntPathRequestMatcher("/notification/token", HttpMethod.DELETE.name())
    }
}
