package com.example.taekbaewatshongserver.domain.auth.service

import com.example.taekbaewatshongserver.domain.auth.entity.RefreshToken
import com.example.taekbaewatshongserver.domain.auth.entity.RefreshTokenRevokeReason
import com.example.taekbaewatshongserver.domain.auth.repository.RefreshTokenRepository
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.RefreshTokenException
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${jwt.refresh-token-validity-ms}") refreshTokenValidityMs: Long,
    @Value("\${jwt.refresh-token-grace-ms}") graceMs: Long,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val secureRandom = SecureRandom()

    private val validity: Duration = Duration.ofMillis(refreshTokenValidityMs)

    private val grace: Duration = Duration.ofMillis(graceMs)

    // 로그인 1회마다 새 family를 만든다. 다른 기기의 family는 건드리지 않는다.
    @Transactional
    fun issueOnLogin(user: User): TokenResponse {
        val userId = requireNotNull(user.id)
        val now = LocalDateTime.now()
        val raw = generate()
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = hash(raw),
                user = user,
                familyId = UUID.randomUUID().toString(),
                expiresAt = now.plus(validity),
                createdAt = now,
            ),
        )
        return TokenResponse(token = jwtTokenProvider.createToken(userId, user.email), refreshToken = raw)
    }

    // 재사용 감지로 family를 폐기한 뒤 401을 던진다. noRollbackFor가 없으면 그 폐기가 함께 롤백된다.
    @Transactional(noRollbackFor = [RefreshTokenException.ReuseDetected::class])
    fun refresh(rawToken: String): TokenResponse {
        if (!TOKEN_PATTERN.matches(rawToken)) {
            throw RefreshTokenException.Invalid()
        }
        val now = LocalDateTime.now()
        val tokenHash = hash(rawToken)

        // UPDATE를 읽기보다 먼저 한다. REPEATABLE READ 스냅샷은 첫 SELECT에서 고정되므로, 먼저 읽으면 경합에서 진 요청이 승자의 커밋을 보지 못한다.
        val rotated = refreshTokenRepository.revokeIfActive(tokenHash, now, RefreshTokenRevokeReason.ROTATED)
        val token = refreshTokenRepository.findWithUserByTokenHash(tokenHash)
            ?: throw RefreshTokenException.Invalid()

        if (rotated == 1) {
            return issueSuccessor(token, now)
        }

        // 폐기되지 않았는데 UPDATE에서 탈락했으면 만료된 토큰이다. LOGOUT·REUSE_DETECTED도 여기서 거절한다.
        if (token.revokeReason != RefreshTokenRevokeReason.ROTATED) {
            throw RefreshTokenException.Invalid()
        }
        val revokedAt = token.revokedAt ?: throw RefreshTokenException.Invalid()

        if (!isWithinGrace(revokedAt, now)) {
            refreshTokenRepository.revokeFamily(token.familyId, now, RefreshTokenRevokeReason.REUSE_DETECTED)
            log.warn("[refresh token 재사용 감지] userId: {}, familyId: {}", token.user.id, token.familyId)
            throw RefreshTokenException.ReuseDetected()
        }

        if (refreshTokenRepository.existsByFamilyIdAndRevokeReasonIn(token.familyId, FAMILY_TERMINATING_REASONS)) {
            throw RefreshTokenException.Invalid()
        }
        // 유예 안의 재제출: 승자가 받은 원문은 해시만 있어 돌려줄 수 없으므로 같은 family에 형제 토큰을 발급한다. 제출된 토큰의 revokedAt은 갱신하지 않는다.
        return issueSuccessor(token, now)
    }

    // 멱등: 형식이 틀리거나, 모르거나, 이미 폐기·만료된 토큰이어도 예외를 던지지 않는다.
    @Transactional
    fun logout(rawToken: String) {
        if (!TOKEN_PATTERN.matches(rawToken)) {
            return
        }
        val token = refreshTokenRepository.findByTokenHash(hash(rawToken)) ?: return
        refreshTokenRepository.revokeFamily(token.familyId, LocalDateTime.now(), RefreshTokenRevokeReason.LOGOUT)
    }

    // 만료는 항상 now + 30일이다(슬라이딩). 이전 토큰의 남은 기간과 무관하다.
    private fun issueSuccessor(previous: RefreshToken, now: LocalDateTime): TokenResponse {
        val user = previous.user
        val raw = generate()
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = hash(raw),
                user = user,
                familyId = previous.familyId,
                expiresAt = now.plus(validity),
                createdAt = now,
            ),
        )
        return TokenResponse(token = jwtTokenProvider.createToken(requireNotNull(user.id), user.email), refreshToken = raw)
    }

    // 경계는 엄격한 미만이다. 정확히 10초가 지난 재제출은 재사용으로 본다.
    internal fun isWithinGrace(revokedAt: LocalDateTime, now: LocalDateTime): Boolean = Duration.between(revokedAt, now) < grace

    internal fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    // MessageDigest는 스레드 안전하지 않으므로 호출마다 새로 만든다.
    internal fun hash(raw: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(raw.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }

    companion object {
        private const val TOKEN_BYTES = 32
        private const val HASH_ALGORITHM = "SHA-256"
        private val TOKEN_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
        private val FAMILY_TERMINATING_REASONS = setOf(RefreshTokenRevokeReason.LOGOUT, RefreshTokenRevokeReason.REUSE_DETECTED)
    }
}
