package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class OneTimeAuthCodeStoreTest {

    private val store = OneTimeAuthCodeStore()
    private val tokens = TokenResponse(token = "access-jwt", refreshToken = "q3J9vX0bA1c2D3e4F5g6H7i8J9k0L1m2N3o4P5q6R7s")

    @Test
    fun `발급한 code를 교환하면 액세스 토큰과 refresh token을 함께 돌려준다`() {
        val code = store.issue(tokens)

        assertSame(tokens, store.consume(code))
    }

    @Test
    fun `같은 code는 한 번만 교환할 수 있다`() {
        val code = store.issue(tokens)
        store.consume(code)

        assertNull(store.consume(code))
    }

    @Test
    fun `모르는 code는 null이다`() {
        assertNull(store.consume("3f2b8c1e-5d7a-4e9b-9c0d-1a2b3c4d5e6f"))
    }

    @Test
    fun `발급할 때마다 다른 code를 만든다`() {
        assertNotEquals(store.issue(tokens), store.issue(tokens))
    }
}
