package com.example.taekbaewatshongserver.domain.auth.repository

import com.example.taekbaewatshongserver.domain.auth.entity.RefreshToken
import com.example.taekbaewatshongserver.domain.auth.entity.RefreshTokenRevokeReason
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :tokenHash")
    fun findWithUserByTokenHash(@Param("tokenHash") tokenHash: String): RefreshToken?

    @Modifying(flushAutomatically = true)
    @Query(
        """
        update RefreshToken t
        set t.revokedAt = :now, t.revokeReason = :reason
        where t.tokenHash = :tokenHash
          and t.revokedAt is null
          and t.expiresAt > :now
        """,
    )
    fun revokeIfActive(
        @Param("tokenHash") tokenHash: String,
        @Param("now") now: LocalDateTime,
        @Param("reason") reason: RefreshTokenRevokeReason,
    ): Int

    @Modifying(flushAutomatically = true)
    @Query(
        """
        update RefreshToken t
        set t.revokedAt = :now, t.revokeReason = :reason
        where t.familyId = :familyId
          and t.revokedAt is null
        """,
    )
    fun revokeFamily(
        @Param("familyId") familyId: String,
        @Param("now") now: LocalDateTime,
        @Param("reason") reason: RefreshTokenRevokeReason,
    ): Int

    fun existsByFamilyIdAndRevokeReasonIn(familyId: String, reasons: Collection<RefreshTokenRevokeReason>): Boolean
}
