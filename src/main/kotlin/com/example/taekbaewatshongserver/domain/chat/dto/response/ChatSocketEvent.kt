package com.example.taekbaewatshongserver.domain.chat.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "실시간 채팅 이벤트 (STOMP `/user/queue/chat`으로 전달)")
data class ChatSocketEvent(
    @field:Schema(description = "이벤트 종류. MESSAGE(새 메시지) / READ(메시지 읽음)", example = "MESSAGE")
    val type: ChatSocketEventType,
    @field:Schema(description = "채팅방 ID", example = "1")
    val roomId: Long,
    @field:Schema(description = "새 메시지 (MESSAGE일 때만). `isMine`은 이벤트를 받는 사람 기준이다.")
    val message: ChatMessageResponse? = null,
    @field:Schema(description = "메시지를 읽은 사람의 사용자 ID (READ일 때만)", example = "2")
    val readerId: Long? = null,
    @field:Schema(description = "이벤트를 받는 사람 기준 이 방의 안 읽은 메시지 수", example = "1")
    val unreadCount: Int,
    @field:Schema(description = "이벤트를 받는 사람 기준 모든 방의 안 읽은 메시지 수 합계", example = "3")
    val totalUnreadCount: Int,
)

enum class ChatSocketEventType {
    MESSAGE, // 새 메시지 도착 (보낸 사람의 다른 기기에도 전달)
    READ, // 방의 메시지가 읽음 처리됨
}
