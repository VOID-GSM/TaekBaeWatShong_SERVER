package com.example.taekbaewatshongserver.domain.chat.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ChatException
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "chat_rooms",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_chat_room_participants",
            columnNames = ["participant_low_id", "participant_high_id"],
        ),
    ],
)
class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: ChatRoomType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_low_id", nullable = false, updatable = false)
    val participantLow: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_high_id", nullable = false, updatable = false)
    val participantHigh: User,

    @Column(length = MAX_PREVIEW_LENGTH)
    var lastMessage: String? = null,

    var lastMessageAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun contains(user: User): Boolean = user.id == participantLow.id || user.id == participantHigh.id

    fun partnerOf(user: User): User = when (user.id) {
        participantLow.id -> participantHigh
        participantHigh.id -> participantLow
        else -> throw ChatException.NotParticipant()
    }

    companion object {
        private const val MAX_PREVIEW_LENGTH = 200

        fun previewOf(content: String): String = content.take(MAX_PREVIEW_LENGTH)

        fun of(type: ChatRoomType, user: User, partner: User): ChatRoom {
            val (low, high) = orderPair(user, partner)
            return ChatRoom(type = type, participantLow = low, participantHigh = high)
        }

        fun orderPair(user: User, partner: User): Pair<User, User> {
            val userId = requireNotNull(user.id) { "저장되지 않은 사용자로는 채팅방을 만들 수 없습니다." }
            val partnerId = requireNotNull(partner.id) { "저장되지 않은 사용자로는 채팅방을 만들 수 없습니다." }
            return if (userId <= partnerId) user to partner else partner to user
        }
    }
}
