package com.example.taekbaewatshongserver.domain.admin.service

import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCode
import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCodeUsage
import com.example.taekbaewatshongserver.domain.admin.repository.AdminInviteCodeRepository
import com.example.taekbaewatshongserver.domain.admin.repository.AdminInviteCodeUsageRepository
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AdminInviteCodeServiceTest {

    private val admin = User(id = 10L, email = "admin@gsm.hs.kr", name = "관리자", provider = AuthProvider.LOCAL, role = Role.ADMIN)
    private val student = User(id = 1L, email = "student@gsm.hs.kr", name = "이학생", provider = AuthProvider.GOOGLE, role = Role.STUDENT)

    private val adminInviteCodeRepository: AdminInviteCodeRepository = mock(AdminInviteCodeRepository::class.java)
    private val adminInviteCodeUsageRepository: AdminInviteCodeUsageRepository = mock(AdminInviteCodeUsageRepository::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val hashSecret = "test-invite-code-hmac-secret-0123456789abcdef"
    private val service = serviceWith(hashSecret)

    private fun serviceWith(secret: String) = AdminInviteCodeService(
        adminInviteCodeRepository,
        adminInviteCodeUsageRepository,
        userRepository,
        secret,
    )

    private val code = "K7Q2M9XA"
    private val codeHash = service.hash(code)

    private fun inviteCodeOf(expiresAt: LocalDateTime, maxUses: Int = 1, usedCount: Int = 0) = AdminInviteCode(
        id = 1L,
        codeHash = codeHash,
        createdBy = admin,
        expiresAt = expiresAt,
        maxUses = maxUses,
        usedCount = usedCount,
    )

    // Mockito 매처는 null을 돌려주므로, Kotlin의 non-null 인자 검사에 걸리지 않게 실제 값을 대신 돌려준다.
    private fun anyTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    private fun eqString(value: String): String {
        ArgumentMatchers.eq(value)
        return value
    }

    private fun sameUser(user: User): User {
        ArgumentMatchers.same(user)
        return user
    }

    private fun anyInviteCode(): AdminInviteCode {
        ArgumentMatchers.any(AdminInviteCode::class.java)
        return inviteCodeOf(LocalDateTime.MAX)
    }

    private fun anyUsage(): AdminInviteCodeUsage {
        ArgumentMatchers.any(AdminInviteCodeUsage::class.java)
        return AdminInviteCodeUsage(inviteCode = inviteCodeOf(LocalDateTime.MAX), usedBy = student)
    }

    @Test
    fun `생성된 코드는 헷갈리는 문자 없이 XXXX-XXXX 형식이다`() {
        repeat(200) {
            val generated = service.generateCode()
            assertTrue(Regex("^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$").matches(generated), generated)
        }
    }

    @Test
    fun `하이픈·공백·대소문자가 달라도 같은 해시가 된다`() {
        val expected = service.hash(service.normalize("K7Q2-M9XA"))

        assertEquals(expected, service.hash(service.normalize("k7q2-m9xa")))
        assertEquals(expected, service.hash(service.normalize(" K7Q2 M9XA ")))
        assertEquals(expected, service.hash(service.normalize("K7Q2M9XA")))
        assertTrue(Regex("^[0-9a-f]{64}$").matches(expected))
    }

    @Test
    fun `해시는 jwt secret을 키로 쓴 HMAC-SHA256 소문자 hex다`() {
        assertEquals("4afe00e228f7140550f05953cbc716e00a4bc07e39f97642c0327ba21258e30d", service.hash("ABCDEFGH"))
        assertEquals(service.hash("ABCDEFGH"), serviceWith(hashSecret).hash("ABCDEFGH"))
    }

    @Test
    fun `키가 다르면 해시가 달라지고 키 없는 SHA-256과도 다르다`() {
        val plainSha256 = "9ac2197d9258257b1ae8463e4214e4cd0a578bc1517f2415928b91be4283fc48"

        assertNotEquals(service.hash("ABCDEFGH"), serviceWith("another-invite-code-hmac-secret-9876543210zyx").hash("ABCDEFGH"))
        assertNotEquals(plainSha256, service.hash("ABCDEFGH"))
    }

    @Test
    fun `발급하면 해시와 인원 제한만 저장하고 원문 코드와 24시간 뒤 만료 시각을 돌려준다`() {
        `when`(adminInviteCodeRepository.save(anyInviteCode())).thenAnswer { it.getArgument<AdminInviteCode>(0) }
        val before = LocalDateTime.now()

        val response = service.issue(admin, 5)

        val captor = ArgumentCaptor.forClass(AdminInviteCode::class.java)
        verify(adminInviteCodeRepository).save(captor.capture())
        val saved = captor.value
        assertEquals(service.hash(service.normalize(response.code)), saved.codeHash)
        assertSame(admin, saved.createdBy)
        assertEquals(5, saved.maxUses)
        assertEquals(0, saved.usedCount)
        assertEquals(5, response.maxUses)
        assertEquals(saved.expiresAt, response.expiresAt)
        assertEquals(Duration.ofHours(24), Duration.between(saved.createdAt, saved.expiresAt))
        assertTrue(!saved.createdAt.isBefore(before))
    }

    @Test
    fun `인원 제한은 1명부터 20명까지 허용한다`() {
        `when`(adminInviteCodeRepository.save(anyInviteCode())).thenAnswer { it.getArgument<AdminInviteCode>(0) }

        assertEquals(AdminInviteCodeService.DEFAULT_MAX_USES, service.issue(admin, AdminInviteCodeService.DEFAULT_MAX_USES).maxUses)
        assertEquals(1, AdminInviteCodeService.DEFAULT_MAX_USES)
        assertEquals(20, service.issue(admin, AdminInviteCodeService.MAX_USES_LIMIT).maxUses)
    }

    @Test
    fun `인원 제한이 범위를 벗어나면 저장하지 않고 InvalidMaxUses를 던진다`() {
        listOf(0, -1, 21).forEach { maxUses ->
            assertFailsWith<AdminInviteCodeException.InvalidMaxUses>(maxUses.toString()) { service.issue(admin, maxUses) }
        }
        verify(adminInviteCodeRepository, never()).save(ArgumentMatchers.any(AdminInviteCode::class.java))
    }

    @Test
    fun `형식이 잘못된 코드는 조회 전에 InvalidFormat을 던진다`() {
        listOf("K7Q2M9X", "K7Q2M9XA1", "K7Q2M9X0", "K7Q2M9XO", "K7Q2M9XI", "K7Q2M9X!", "", "   ").forEach { raw ->
            assertFailsWith<AdminInviteCodeException.InvalidFormat>(raw) { service.validate(raw) }
            assertFailsWith<AdminInviteCodeException.InvalidFormat>(raw) { service.consume(raw, student) }
        }
        verify(adminInviteCodeRepository, never()).findByCodeHash(ArgumentMatchers.anyString())
        verify(userRepository, never()).save(ArgumentMatchers.any(User::class.java))
    }

    @Test
    fun `유효한 코드를 확인하면 정규화된 코드를 돌려주고 소비하지 않는다`() {
        `when`(adminInviteCodeRepository.findByCodeHash(codeHash))
            .thenReturn(inviteCodeOf(LocalDateTime.now().plusHours(1), maxUses = 3, usedCount = 2))

        assertEquals(code, service.validate("k7q2-m9xa"))
        verify(adminInviteCodeRepository, never()).markUsed(ArgumentMatchers.anyString(), anyTime())
        verify(adminInviteCodeUsageRepository, never()).save(ArgumentMatchers.any(AdminInviteCodeUsage::class.java))
    }

    @Test
    fun `없거나 인원이 다 찼거나 만료된 코드를 확인하면 Invalid를 던진다`() {
        `when`(adminInviteCodeRepository.findByCodeHash(codeHash)).thenReturn(null)
        assertFailsWith<AdminInviteCodeException.Invalid> { service.validate(code) }

        `when`(adminInviteCodeRepository.findByCodeHash(codeHash))
            .thenReturn(inviteCodeOf(LocalDateTime.now().plusHours(1), maxUses = 3, usedCount = 3))
        assertFailsWith<AdminInviteCodeException.Invalid> { service.validate(code) }

        `when`(adminInviteCodeRepository.findByCodeHash(codeHash)).thenReturn(inviteCodeOf(LocalDateTime.now().minusSeconds(1)))
        assertFailsWith<AdminInviteCodeException.Invalid> { service.validate(code) }
    }

    @Test
    fun `사용 가능 여부는 남은 인원과 만료 시각으로 판단한다`() {
        val expiresAt = LocalDateTime.of(2026, 11, 27, 12, 0)

        assertTrue(inviteCodeOf(expiresAt, maxUses = 3, usedCount = 2).isUsable(expiresAt.minusNanos(1)))
        assertFalse(inviteCodeOf(expiresAt, maxUses = 3, usedCount = 3).isUsable(expiresAt.minusHours(1)))
        assertFalse(inviteCodeOf(expiresAt, maxUses = 3, usedCount = 0).isUsable(expiresAt))
    }

    @Test
    fun `소비에 성공하면 ADMIN으로 승격해 저장하고 사용 기록을 1행 남긴다`() {
        val persisted = User(id = 1L, email = student.email, name = student.name, provider = AuthProvider.GOOGLE, role = Role.ADMIN)
        val inviteCode = inviteCodeOf(LocalDateTime.now().plusHours(1), maxUses = 3, usedCount = 1)
        `when`(userRepository.save(sameUser(student))).thenReturn(persisted)
        `when`(adminInviteCodeRepository.markUsed(eqString(codeHash), anyTime())).thenReturn(1)
        `when`(adminInviteCodeRepository.findByCodeHash(codeHash)).thenReturn(inviteCode)

        val actual = service.consume("k7q2-m9xa", student)

        assertSame(persisted, actual)
        assertEquals(Role.ADMIN, student.role)
        val captor = ArgumentCaptor.forClass(AdminInviteCodeUsage::class.java)
        verify(adminInviteCodeUsageRepository, times(1)).save(captor.capture())
        assertSame(inviteCode, captor.value.inviteCode)
        assertSame(persisted, captor.value.usedBy)
    }

    @Test
    fun `여러 명이 쓰면 소비할 때마다 사용 기록이 1행씩 생긴다`() {
        val other = User(id = 2L, email = "other@gsm.hs.kr", name = "김학생", provider = AuthProvider.GOOGLE, role = Role.STUDENT)
        `when`(userRepository.save(sameUser(student))).thenReturn(student)
        `when`(userRepository.save(sameUser(other))).thenReturn(other)
        `when`(adminInviteCodeRepository.markUsed(eqString(codeHash), anyTime())).thenReturn(1)
        `when`(adminInviteCodeRepository.findByCodeHash(codeHash)).thenReturn(inviteCodeOf(LocalDateTime.now().plusHours(1), maxUses = 2))

        service.consume(code, student)
        service.consume(code, other)

        val captor = ArgumentCaptor.forClass(AdminInviteCodeUsage::class.java)
        verify(adminInviteCodeUsageRepository, times(2)).save(captor.capture())
        assertEquals(listOf(student, other), captor.allValues.map { it.usedBy })
    }

    @Test
    fun `인원이 다 찼거나 만료되어 조건부 UPDATE가 0행이면 Invalid를 던지고 사용 기록을 남기지 않는다`() {
        `when`(userRepository.save(sameUser(student))).thenReturn(student)
        `when`(adminInviteCodeRepository.markUsed(eqString(codeHash), anyTime())).thenReturn(0)

        assertFailsWith<AdminInviteCodeException.Invalid> { service.consume(code, student) }
        verify(adminInviteCodeUsageRepository, never()).save(anyUsage())
    }
}
