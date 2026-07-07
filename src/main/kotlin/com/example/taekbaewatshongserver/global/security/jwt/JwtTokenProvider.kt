package com.example.taekbaewatshongserver.global.security.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-validity-ms}") private val validityMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createToken(userId: Long, email: String): String {
        val now = Date()
        val expiry = Date(now.time + validityMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    fun getUserId(token: String): Long =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token)
            .payload
            .subject
            .toLong()
}