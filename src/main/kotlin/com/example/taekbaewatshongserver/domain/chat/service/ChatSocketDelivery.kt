package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEvent

/** 커밋 후 [recipientId]의 개인 큐로 보낼 실시간 이벤트. 내용은 커밋 전에 요청 트랜잭션 안에서 만들어 둔다. */
data class ChatSocketDelivery(
    val recipientId: Long,
    val payload: ChatSocketEvent,
)
