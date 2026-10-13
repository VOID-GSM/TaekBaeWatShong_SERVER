package com.example.taekbaewatshongserver.domain.chat.dto.response

data class ChatMessageListResponse(
    val messages: List<ChatMessageResponse>,
    val nextCursor: Long? = null,
)
