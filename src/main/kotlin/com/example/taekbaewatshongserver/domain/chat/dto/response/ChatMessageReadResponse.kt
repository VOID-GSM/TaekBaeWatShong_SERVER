package com.example.taekbaewatshongserver.domain.chat.dto.response

data class ChatMessageReadResponse(
    val roomId: Long,
    val readCount: Int,
    val unreadCount: Int,
)
