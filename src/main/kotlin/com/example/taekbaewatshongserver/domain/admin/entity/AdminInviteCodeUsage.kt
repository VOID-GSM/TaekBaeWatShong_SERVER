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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "admin_invite_code_usages",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_admin_invite_code_usages_code_user", columnNames = ["invite_code_id", "used_by"]),
    ],
)
class AdminInviteCodeUsage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_code_id", nullable = false, updatable = false)
    val inviteCode: AdminInviteCode,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by", nullable = false, updatable = false)
    val usedBy: User,

    @Column(nullable = false, updatable = false)
    val usedAt: LocalDateTime = LocalDateTime.now(),
)
