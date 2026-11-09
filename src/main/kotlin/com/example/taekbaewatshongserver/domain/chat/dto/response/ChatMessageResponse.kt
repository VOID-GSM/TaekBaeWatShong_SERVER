package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "채팅 메시지")
data class ChatMessageResponse(
    @field:Schema(description = "메시지 ID", example = "250")
    val id: Long,
    @field:Schema(description = "채팅방 ID", example = "1")
    val roomId: Long,
    @field:Schema(description = "보낸 사람 사용자 ID", example = "2")
    val senderId: Long,
    @field:Schema(description = "보낸 사람 이름", example = "김택배")
    val senderName: String,
    @field:Schema(description = "메시지 내용", example = "택배 어디에 두셨나요?")
    val content: String,
    @field:Schema(description = "내가 보낸 메시지인지 여부", example = "true")
    val isMine: Boolean,
    @field:Schema(description = "읽음 여부. 내가 보낸 메시지면 상대가 읽었는지, 받은 메시지면 내가 읽었는지를 뜻한다.", example = "false")
    val isRead: Boolean,
    @field:Schema(description = "보낸 시각", example = "2026-11-09T14:30:00")
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
