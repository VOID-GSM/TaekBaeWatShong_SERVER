package com.example.taekbaewatshongserver.domain.chat.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 채팅방 목록")
data class ChatRoomListResponse(
    @field:Schema(description = "모든 채팅방의 안 읽은 메시지 수 합계", example = "5")
    val totalUnreadCount: Int,
    @field:Schema(description = "채팅방 목록 (최근 메시지 순)")
    val rooms: List<ChatRoomResponse>,
)
