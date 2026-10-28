package com.example.taekbaewatshongserver.domain.chat.dto.response

data class ChatRoomCreateResult(
    val room: ChatRoomResponse,
    val created: Boolean,
)
