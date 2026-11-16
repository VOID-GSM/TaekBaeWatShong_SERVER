package com.example.taekbaewatshongserver.domain.chat.service

data class ChatMessageSentEvent(
    val messageId: Long,
)

data class ChatMessagesReadEvent(
    val roomId: Long,
    val readerId: Long,
    val readCount: Int,
)
