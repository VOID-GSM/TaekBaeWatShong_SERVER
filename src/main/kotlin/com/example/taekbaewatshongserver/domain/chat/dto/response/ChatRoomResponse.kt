package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.user.entity.User
import java.time.LocalDateTime

data class ChatRoomResponse(
    val id: Long,
    val type: ChatRoomType,
    val partner: ChatPartnerResponse,
    val lastMessage: String? = null,
    val lastMessageAt: LocalDateTime? = null,
    val unreadCount: Int = 0,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(room: ChatRoom, viewer: User, unreadCount: Int = 0): ChatRoomResponse = ChatRoomResponse(
            id = room.id,
            type = room.type,
            partner = ChatPartnerResponse.from(room.partnerOf(viewer)),
            lastMessage = room.lastMessage,
            lastMessageAt = room.lastMessageAt,
            unreadCount = unreadCount,
            createdAt = room.createdAt,
        )
    }
}
