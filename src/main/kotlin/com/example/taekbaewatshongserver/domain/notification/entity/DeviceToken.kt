package com.example.taekbaewatshongserver.domain.notification.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "device_tokens")
class DeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true)
    var deviceToken: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var platform: DevicePlatform,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var owner: User,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun reassign(newOwner: User, newPlatform: DevicePlatform) {
        this.owner = newOwner
        this.platform = newPlatform
    }
}
