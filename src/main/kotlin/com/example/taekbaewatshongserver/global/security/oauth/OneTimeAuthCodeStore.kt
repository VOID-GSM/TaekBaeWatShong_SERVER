package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class OneTimeAuthCodeStore {
    private data class Entry(val tokens: TokenResponse, val expiresAt: Instant)

    private val store = ConcurrentHashMap<String, Entry>()

    fun issue(tokens: TokenResponse): String {
        purgeExpired()
        val code = UUID.randomUUID().toString()
        store[code] = Entry(tokens, Instant.now().plusSeconds(TTL_SECONDS))
        return code
    }

    fun consume(code: String): TokenResponse? {
        purgeExpired()
        val entry = store.remove(code) ?: return null
        return entry.tokens.takeIf { entry.expiresAt.isAfter(Instant.now()) }
    }

    private fun purgeExpired() {
        val now = Instant.now()
        store.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    companion object {
        private const val TTL_SECONDS = 30L
    }
}
