package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "채팅방")
data class ChatRoomResponse(
    @field:Schema(description = "채팅방 ID", example = "1")
    val id: Long,
    @field:Schema(description = "채팅방 종류. DRIVER(택배기사 ↔ 학생·선생님) / CLIENT(학생 ↔ 선생님)", example = "DRIVER")
    val type: ChatRoomType,
    @field:Schema(description = "대화 상대")
    val partner: ChatPartnerResponse,
    @field:Schema(description = "마지막 메시지 미리보기 (최대 200자, 메시지가 없으면 null)", example = "택배 어디에 두셨나요?")
    val lastMessage: String? = null,
    @field:Schema(description = "마지막 메시지 시각 (메시지가 없으면 null)", example = "2026-11-09T14:30:00")
    val lastMessageAt: LocalDateTime? = null,
    @field:Schema(description = "이 방의 안 읽은 메시지 수 (새로 만든 방은 0)", example = "2")
    val unreadCount: Int = 0,
    @field:Schema(description = "채팅방 생성 시각", example = "2026-11-01T10:00:00")
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
