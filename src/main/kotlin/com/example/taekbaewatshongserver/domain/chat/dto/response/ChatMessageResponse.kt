package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.user.entity.User
import java.time.LocalDateTime

data class ChatMessageResponse(
    val id: Long,
    val roomId: Long,
    val senderId: Long,
    val senderName: String,
    val content: String,
    val isMine: Boolean,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(message: ChatMessage, viewer: User): ChatMessageResponse = ChatMessageResponse(
            id = message.id,
            roomId = message.room.id,
            senderId = requireNotNull(message.sender.id),
            senderName = message.sender.name,
            content = message.content,
            isMine = message.sender.id == viewer.id,
            isRead = message.isRead,
            createdAt = message.createdAt,
        )
    }
}
