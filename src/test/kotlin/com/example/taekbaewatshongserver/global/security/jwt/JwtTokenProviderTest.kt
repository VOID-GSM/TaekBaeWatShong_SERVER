package com.example.taekbaewatshongserver.global.security.jwt

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtTokenProviderTest {

    private val provider = JwtTokenProvider(SECRET, 60_000L)

    // 유효 기간을 음수로 두면 만들자마자 만료된 토큰이 나온다.
    private val expiredProvider = JwtTokenProvider(SECRET, -60_000L)

    @Test
    fun `유효한 토큰은 두 방법 모두 claims를 돌려준다`() {
        val token = provider.createToken(7L, "user@gsm.hs.kr")

        assertEquals("7", provider.getClaims(token)?.subject)
        assertEquals("7", provider.getClaimsAllowingExpired(token)?.subject)
    }

    @Test
    fun `만료된 토큰은 getClaims에서 null이지만 getClaimsAllowingExpired에서는 claims를 돌려준다`() {
        val token = expiredProvider.createToken(7L, "user@gsm.hs.kr")

        assertNull(provider.getClaims(token))
        assertEquals("7", provider.getClaimsAllowingExpired(token)?.subject)
    }

    @Test
    fun `다른 키로 서명한 만료 토큰은 getClaimsAllowingExpired도 받지 않는다`() {
        val forged = JwtTokenProvider(OTHER_SECRET, -60_000L).createToken(7L, "user@gsm.hs.kr")

        assertNull(provider.getClaimsAllowingExpired(forged))
    }

    @Test
    fun `형식이 틀린 토큰은 getClaimsAllowingExpired도 null이다`() {
        assertNull(provider.getClaimsAllowingExpired("not-a-jwt"))
        assertNull(provider.getClaimsAllowingExpired(""))
    }

    companion object {
        private const val SECRET = "test-secret-key-that-is-at-least-32-bytes!!"
        private const val OTHER_SECRET = "another-secret-key-that-is-at-least-32-bytes"
    }
}
