package com.example.taekbaewatshongserver.global.security.jwt

import io.jsonwebtoken.Claims
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
    init {
        require(secret.toByteArray().size >= 32) {
            "jwt.secret은 HS256 서명을 위해 최소 32바이트(256bit) 이상이어야 합니다"
        }
    }

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

    fun getClaims(token: String): Claims? = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    } catch (e: ExpiredJwtException) {
        null
    } catch (e: JwtException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    /**
     * [getClaims]와 같지만 만료된 토큰도 받아 준다. 서명이 맞지 않거나 형식이 틀린 토큰은 그대로 null이다.
     * (jjwt는 서명을 검증한 뒤에 만료를 검사하므로, [ExpiredJwtException]의 claims는 서명이 확인된 값이다.)
     *
     * 만료된 뒤에도 해야 하는 정리 작업(로그아웃 시 디바이스 토큰 삭제)에만 쓴다.
     */
    fun getClaimsAllowingExpired(token: String): Claims? = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    } catch (e: ExpiredJwtException) {
        e.claims
    } catch (e: JwtException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
