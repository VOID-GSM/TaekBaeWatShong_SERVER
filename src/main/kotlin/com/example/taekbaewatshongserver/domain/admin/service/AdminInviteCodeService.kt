package com.example.taekbaewatshongserver.domain.admin.service

import com.example.taekbaewatshongserver.domain.admin.dto.response.AdminInviteCodeIssueResponse
import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCode
import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCodeUsage
import com.example.taekbaewatshongserver.domain.admin.repository.AdminInviteCodeRepository
import com.example.taekbaewatshongserver.domain.admin.repository.AdminInviteCodeUsageRepository
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.util.HexFormat
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
@Transactional(readOnly = true)
class AdminInviteCodeService(
    private val adminInviteCodeRepository: AdminInviteCodeRepository,
    private val adminInviteCodeUsageRepository: AdminInviteCodeUsageRepository,
    private val userRepository: UserRepository,
    @Value("\${jwt.secret}") hashSecret: String,
) {

    private val secureRandom = SecureRandom()

    private val hashKey = SecretKeySpec(hashSecret.toByteArray(Charsets.UTF_8), HASH_ALGORITHM)

    @Transactional
    fun issue(issuer: User, maxUses: Int): AdminInviteCodeIssueResponse {
        if (maxUses !in MIN_MAX_USES..MAX_USES_LIMIT) {
            throw AdminInviteCodeException.InvalidMaxUses()
        }

        val displayCode = generateCode()
        val now = LocalDateTime.now()
        val saved = adminInviteCodeRepository.save(
            AdminInviteCode(
                codeHash = hash(normalize(displayCode)),
                createdBy = issuer,
                expiresAt = now.plus(VALIDITY),
                maxUses = maxUses,
                createdAt = now,
            ),
        )
        return AdminInviteCodeIssueResponse.from(saved, displayCode)
    }

    fun validate(rawCode: String): String {
        val normalized = requireValidFormat(rawCode)
        val inviteCode = adminInviteCodeRepository.findByCodeHash(hash(normalized))
            ?: throw AdminInviteCodeException.Invalid()
        if (!inviteCode.isUsable(LocalDateTime.now())) {
            throw AdminInviteCodeException.Invalid()
        }
        return normalized
    }

    // 호출자의 트랜잭션에 합류한다. 코드가 무효이면 예외로 롤백되어 User 저장·승격도 함께 취소된다. 반드시 반환값을 써야 한다.
    @Transactional
    fun consume(rawCode: String, user: User): User {
        val codeHash = hash(requireValidFormat(rawCode))

        user.role = Role.ADMIN
        val saved = userRepository.save(user)

        val now = LocalDateTime.now()
        if (adminInviteCodeRepository.markUsed(codeHash, now) != 1) {
            throw AdminInviteCodeException.Invalid()
        }

        val inviteCode = adminInviteCodeRepository.findByCodeHash(codeHash)
            ?: throw AdminInviteCodeException.Invalid()
        adminInviteCodeUsageRepository.save(AdminInviteCodeUsage(inviteCode = inviteCode, usedBy = saved, usedAt = now))
        return saved
    }

    internal fun normalize(raw: String): String = raw.filterNot { it == '-' || it.isWhitespace() }.uppercase(Locale.ROOT)

    // Mac은 스레드 안전하지 않으므로 호출마다 새로 만든다. 키는 생성자에서 한 번만 만든다.
    internal fun hash(normalized: String): String {
        val mac = Mac.getInstance(HASH_ALGORITHM).apply { init(hashKey) }
        return HexFormat.of().formatHex(mac.doFinal(normalized.toByteArray(Charsets.UTF_8)))
    }

    internal fun generateCode(): String {
        val raw = String(CharArray(CODE_LENGTH) { ALPHABET[secureRandom.nextInt(ALPHABET.length)] })
        return raw.chunked(DISPLAY_GROUP_SIZE).joinToString("-")
    }

    private fun requireValidFormat(rawCode: String): String {
        val normalized = normalize(rawCode)
        if (!CODE_PATTERN.matches(normalized)) {
            throw AdminInviteCodeException.InvalidFormat()
        }
        return normalized
    }

    companion object {
        val VALIDITY: Duration = Duration.ofHours(24)
        const val DEFAULT_MAX_USES = 1
        const val MAX_USES_LIMIT = 20
        private const val MIN_MAX_USES = 1
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val CODE_LENGTH = 8
        private const val DISPLAY_GROUP_SIZE = 4
        private const val HASH_ALGORITHM = "HmacSHA256"
        private val CODE_PATTERN = Regex("^[$ALPHABET]{$CODE_LENGTH}$")
    }
}
