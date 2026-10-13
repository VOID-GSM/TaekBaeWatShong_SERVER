package com.example.taekbaewatshongserver.domain.chat.dto.response

data class ChatRoomListResponse(
    val totalUnreadCount: Int,
    val rooms: List<ChatRoomResponse>,
)
