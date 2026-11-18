package com.example.taekbaewatshongserver.domain.chat.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "메시지 읽음 처리 결과")
data class ChatMessageReadResponse(
    @field:Schema(description = "채팅방 ID", example = "1")
    val roomId: Long,
    @field:Schema(description = "이번에 읽음 처리된 메시지 수", example = "3")
    val readCount: Int,
    @field:Schema(description = "처리 후 이 방의 안 읽은 메시지 수 (항상 0)", example = "0")
    val unreadCount: Int,
)
