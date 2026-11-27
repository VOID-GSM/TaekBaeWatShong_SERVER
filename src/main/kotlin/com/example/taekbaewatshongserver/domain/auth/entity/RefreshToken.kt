package com.example.taekbaewatshongserver.domain.auth.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [Index(name = "idx_refresh_tokens_family_id", columnList = "family_id")],
)
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,

    @Column(nullable = false, length = 36, updatable = false)
    val familyId: String,

    @Column(nullable = false, updatable = false)
    val expiresAt: LocalDateTime,

    // 폐기는 RefreshTokenRepository의 조건부 UPDATE로만 한다. 원자성을 지키기 위해 일부러 val로 둔다(AdminInviteCode.usedCount와 같은 이유).
    @Column
    val revokedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val revokeReason: RefreshTokenRevokeReason? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
