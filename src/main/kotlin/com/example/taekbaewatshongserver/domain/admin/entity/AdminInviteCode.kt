package com.example.taekbaewatshongserver.domain.admin.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "admin_invite_codes")
class AdminInviteCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    val codeHash: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    val createdBy: User,

    @Column(nullable = false, updatable = false)
    val expiresAt: LocalDateTime,

    @Column(nullable = false, updatable = false)
    val maxUses: Int,

    // 사용 횟수는 AdminInviteCodeRepository.markUsed의 조건부 UPDATE로만 늘린다. 원자성을 지키기 위해 일부러 val로 둔다.
    @Column(nullable = false)
    val usedCount: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isUsable(now: LocalDateTime): Boolean = usedCount < maxUses && expiresAt.isAfter(now)
}
