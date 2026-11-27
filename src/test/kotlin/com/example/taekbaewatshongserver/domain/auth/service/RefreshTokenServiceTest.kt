package com.example.taekbaewatshongserver.domain.auth.service

import com.example.taekbaewatshongserver.domain.auth.entity.RefreshToken
import com.example.taekbaewatshongserver.domain.auth.entity.RefreshTokenRevokeReason
import com.example.taekbaewatshongserver.domain.auth.repository.RefreshTokenRepository
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.RefreshTokenException
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshTokenServiceTest {

    private val user = User(id = 1L, email = "student@gsm.hs.kr", name = "이학생", provider = AuthProvider.GOOGLE, role = Role.STUDENT)

    private val refreshTokenRepository: RefreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val jwtTokenProvider = JwtTokenProvider("test-jwt-secret-for-refresh-token-0123456789", 3_600_000L)
    private val validity: Duration = Duration.ofDays(30)
    private val service = RefreshTokenService(refreshTokenRepository, jwtTokenProvider, validity.toMillis(), 10_000L)

    private val familyId = UUID.randomUUID().toString()
    private val raw = service.generate()
    private val rawHash = service.hash(raw)

    private val saved = mutableListOf<RefreshToken>()

    @BeforeEach
    fun setUp() {
        `when`(refreshTokenRepository.save(anyToken())).thenAnswer { it.getArgument<RefreshToken>(0).also(saved::add) }
    }

    private fun tokenOf(
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(1),
        revokedAt: LocalDateTime? = null,
        revokeReason: RefreshTokenRevokeReason? = null,
    ) = RefreshToken(
        id = 1L,
        tokenHash = rawHash,
        user = user,
        familyId = familyId,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        revokeReason = revokeReason,
    )

    // Mockito 매처는 null을 돌려주므로, Kotlin의 non-null 인자 검사에 걸리지 않게 실제 값을 대신 돌려준다.
    private fun anyTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    private fun anyString(): String {
        ArgumentMatchers.anyString()
        return ""
    }

    private fun eqString(value: String): String {
        ArgumentMatchers.eq(value)
        return value
    }

    private fun eqReason(value: RefreshTokenRevokeReason): RefreshTokenRevokeReason {
        ArgumentMatchers.eq(value)
        return value
    }

    private fun anyReason(): RefreshTokenRevokeReason {
        ArgumentMatchers.any(RefreshTokenRevokeReason::class.java)
        return RefreshTokenRevokeReason.ROTATED
    }

    private fun anyToken(): RefreshToken {
        ArgumentMatchers.any(RefreshToken::class.java)
        return RefreshToken(tokenHash = "", user = user, familyId = "", expiresAt = LocalDateTime.MAX)
    }

    private fun givenStoredToken(token: RefreshToken, rotated: Int = 0) {
        `when`(refreshTokenRepository.revokeIfActive(eqString(rawHash), anyTime(), eqReason(RefreshTokenRevokeReason.ROTATED))).thenReturn(rotated)
        `when`(refreshTokenRepository.findWithUserByTokenHash(rawHash)).thenReturn(token)
    }

    private fun assertExpiresInThirtyDays(token: RefreshToken, before: LocalDateTime, after: LocalDateTime) {
        assertFalse(token.expiresAt.isBefore(before.plus(validity)), "${token.expiresAt}")
        assertFalse(token.expiresAt.isAfter(after.plus(validity)), "${token.expiresAt}")
    }

    private fun verifyNothingIssuedOrRevoked() {
        verify(refreshTokenRepository, never()).save(anyToken())
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), anyTime(), anyReason())
    }

    @Test
    fun `생성된 원문은 43자 URL-safe Base64이고 매번 다르다`() {
        repeat(200) {
            val generated = service.generate()
            assertTrue(Regex("^[A-Za-z0-9_-]{43}$").matches(generated), generated)
        }
        assertNotEquals(service.generate(), service.generate())
    }

    @Test
    fun `해시는 원문의 평문 SHA-256 소문자 hex다`() {
        val hash = service.hash("abc")

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash)
        assertEquals(hash, service.hash("abc"))
        assertTrue(Regex("^[0-9a-f]{64}$").matches(rawHash))
        assertNotEquals(raw, rawHash)
    }

    @Test
    fun `로그인 발급은 해시만 저장하고 새 family로 30일 만료 토큰을 만든다`() {
        val before = LocalDateTime.now()
        val first = service.issueOnLogin(user)
        val second = service.issueOnLogin(user)
        val after = LocalDateTime.now()

        assertEquals(2, saved.size)
        val stored = saved[0]
        assertEquals(service.hash(first.refreshToken), stored.tokenHash)
        assertEquals(service.hash(second.refreshToken), saved[1].tokenHash)
        assertExpiresInThirtyDays(stored, before, after)
        assertEquals(stored.familyId, UUID.fromString(stored.familyId).toString())
        assertNotEquals(stored.familyId, saved[1].familyId)
        assertEquals(null, stored.revokedAt)
        assertEquals(null, stored.revokeReason)
        assertEquals("1", jwtTokenProvider.getClaims(first.token)?.subject)
    }

    @Test
    fun `정상 재발급은 UPDATE를 먼저 하고 같은 family에 30일 만료 새 토큰을 발급한다`() {
        givenStoredToken(tokenOf(expiresAt = LocalDateTime.now().plusHours(1)), rotated = 1)

        val before = LocalDateTime.now()
        val response = service.refresh(raw)
        val after = LocalDateTime.now()

        val successor = saved.single()
        assertEquals(familyId, successor.familyId)
        assertEquals(service.hash(response.refreshToken), successor.tokenHash)
        assertNotEquals(rawHash, successor.tokenHash)
        assertExpiresInThirtyDays(successor, before, after)
        assertEquals("1", jwtTokenProvider.getClaims(response.token)?.subject)

        val order = inOrder(refreshTokenRepository)
        order.verify(refreshTokenRepository).revokeIfActive(eqString(rawHash), anyTime(), eqReason(RefreshTokenRevokeReason.ROTATED))
        order.verify(refreshTokenRepository).findWithUserByTokenHash(rawHash)
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), anyTime(), anyReason())
    }

    @Test
    fun `형식이 틀린 값은 DB를 조회하지 않고 Invalid다`() {
        listOf("", raw.dropLast(1), raw.dropLast(1) + "+", raw.dropLast(1) + "=", "$raw\n").forEach { malformed ->
            assertFailsWith<RefreshTokenException.Invalid>(malformed) { service.refresh(malformed) }
        }
        verifyNoInteractions(refreshTokenRepository)
    }

    @Test
    fun `모르는 토큰은 Invalid다`() {
        assertFailsWith<RefreshTokenException.Invalid> { service.refresh(raw) }
        verifyNothingIssuedOrRevoked()
    }

    @Test
    fun `폐기되지 않았지만 만료된 토큰은 Invalid다`() {
        givenStoredToken(tokenOf(expiresAt = LocalDateTime.now().minusMinutes(1)))

        assertFailsWith<RefreshTokenException.Invalid> { service.refresh(raw) }
        verifyNothingIssuedOrRevoked()
    }

    @Test
    fun `회전 직후 유예 안의 재제출은 같은 family에 형제 토큰을 발급한다`() {
        givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusSeconds(5), revokeReason = RefreshTokenRevokeReason.ROTATED))

        val response = service.refresh(raw)

        val sibling = saved.single()
        assertEquals(familyId, sibling.familyId)
        assertEquals(service.hash(response.refreshToken), sibling.tokenHash)
        verify(refreshTokenRepository).existsByFamilyIdAndRevokeReasonIn(
            familyId,
            setOf(RefreshTokenRevokeReason.LOGOUT, RefreshTokenRevokeReason.REUSE_DETECTED),
        )
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), anyTime(), anyReason())
    }

    @Test
    fun `유예 안이어도 family가 이미 종료됐으면 Invalid다`() {
        givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusSeconds(5), revokeReason = RefreshTokenRevokeReason.ROTATED))
        `when`(refreshTokenRepository.existsByFamilyIdAndRevokeReasonIn(eqString(familyId), ArgumentMatchers.anyCollection())).thenReturn(true)

        assertFailsWith<RefreshTokenException.Invalid> { service.refresh(raw) }
        verifyNothingIssuedOrRevoked()
    }

    @Test
    fun `유예 밖에서 회전된 토큰이 다시 오면 family 전체를 폐기하고 ReuseDetected다`() {
        givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusSeconds(11), revokeReason = RefreshTokenRevokeReason.ROTATED))

        assertFailsWith<RefreshTokenException.ReuseDetected> { service.refresh(raw) }
        verify(refreshTokenRepository).revokeFamily(eqString(familyId), anyTime(), eqReason(RefreshTokenRevokeReason.REUSE_DETECTED))
        verify(refreshTokenRepository, never()).save(anyToken())
    }

    @Test
    fun `정확히 10초가 지나면 유예 밖이다`() {
        val revokedAt = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
        assertTrue(service.isWithinGrace(revokedAt, revokedAt.plusNanos(9_999_999_999)))
        assertFalse(service.isWithinGrace(revokedAt, revokedAt.plusSeconds(10)))

        givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusSeconds(10), revokeReason = RefreshTokenRevokeReason.ROTATED))
        assertFailsWith<RefreshTokenException.ReuseDetected> { service.refresh(raw) }
        verify(refreshTokenRepository).revokeFamily(eqString(familyId), anyTime(), eqReason(RefreshTokenRevokeReason.REUSE_DETECTED))
    }

    @Test
    fun `LOGOUT이나 REUSE_DETECTED로 폐기된 토큰은 상태를 바꾸지 않고 Invalid다`() {
        listOf(RefreshTokenRevokeReason.LOGOUT, RefreshTokenRevokeReason.REUSE_DETECTED).forEach { reason ->
            givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusMinutes(1), revokeReason = reason))
            assertFailsWith<RefreshTokenException.Invalid>(reason.name) { service.refresh(raw) }
        }
        verifyNothingIssuedOrRevoked()
    }

    @Test
    fun `로그아웃한 토큰으로 재발급하면 Invalid다`() {
        givenStoredToken(tokenOf(revokedAt = LocalDateTime.now().minusSeconds(1), revokeReason = RefreshTokenRevokeReason.LOGOUT))

        assertFailsWith<RefreshTokenException.Invalid> { service.refresh(raw) }
        verifyNothingIssuedOrRevoked()
    }

    @Test
    fun `refresh는 ReuseDetected에서 롤백하지 않는다`() {
        val transactional = RefreshTokenService::class.java.getMethod("refresh", String::class.java)
            .getAnnotation(Transactional::class.java)

        assertNotNull(transactional)
        assertFalse(transactional.readOnly)
        assertTrue(RefreshTokenException.ReuseDetected::class.java in transactional.noRollbackFor.map { it.java })
    }

    @Test
    fun `쓰기 메서드는 모두 쓰기 트랜잭션으로 열린다`() {
        listOf(
            RefreshTokenService::class.java.getMethod("issueOnLogin", User::class.java),
            RefreshTokenService::class.java.getMethod("logout", String::class.java),
        ).forEach { method ->
            val transactional = method.getAnnotation(Transactional::class.java)
            assertNotNull(transactional, method.name)
            assertFalse(transactional.readOnly, method.name)
        }
    }

    @Test
    fun `로그아웃은 제출된 토큰의 family 전체를 LOGOUT으로 폐기한다`() {
        `when`(refreshTokenRepository.findByTokenHash(rawHash)).thenReturn(tokenOf())

        service.logout(raw)

        verify(refreshTokenRepository).revokeFamily(eqString(familyId), anyTime(), eqReason(RefreshTokenRevokeReason.LOGOUT))
    }

    @Test
    fun `로그아웃은 이미 폐기·만료된 토큰이어도 예외 없이 family를 폐기한다`() {
        `when`(refreshTokenRepository.findByTokenHash(rawHash)).thenReturn(
            tokenOf(expiresAt = LocalDateTime.now().minusDays(1), revokedAt = LocalDateTime.now().minusDays(2), revokeReason = RefreshTokenRevokeReason.ROTATED),
        )
        `when`(refreshTokenRepository.revokeFamily(anyString(), anyTime(), anyReason())).thenReturn(0)

        service.logout(raw)

        verify(refreshTokenRepository).revokeFamily(eqString(familyId), anyTime(), eqReason(RefreshTokenRevokeReason.LOGOUT))
    }

    @Test
    fun `로그아웃은 모르는 토큰이나 틀린 형식이어도 예외 없이 끝난다`() {
        service.logout(raw)
        verify(refreshTokenRepository).findByTokenHash(rawHash)

        service.logout("")
        service.logout("not-a-refresh-token")

        verify(refreshTokenRepository, never()).revokeFamily(anyString(), anyTime(), anyReason())
    }
}
