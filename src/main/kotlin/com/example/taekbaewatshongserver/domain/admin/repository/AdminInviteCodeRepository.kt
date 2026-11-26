package com.example.taekbaewatshongserver.domain.admin.repository

import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AdminInviteCodeRepository : JpaRepository<AdminInviteCode, Long> {
    fun findByCodeHash(codeHash: String): AdminInviteCode?

    @Modifying(flushAutomatically = true)
    @Query(
        """
        update AdminInviteCode c
        set c.usedCount = c.usedCount + 1
        where c.codeHash = :codeHash
          and c.usedCount < c.maxUses
          and c.expiresAt > :now
        """,
    )
    fun markUsed(
        @Param("codeHash") codeHash: String,
        @Param("now") now: LocalDateTime,
    ): Int
}
