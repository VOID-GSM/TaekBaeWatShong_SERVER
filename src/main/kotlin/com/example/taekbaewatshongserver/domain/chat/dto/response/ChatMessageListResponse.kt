package com.example.taekbaewatshongserver.domain.chat.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "메시지 목록 (커서 페이지)")
data class ChatMessageListResponse(
    @field:Schema(description = "메시지 목록 (최신순)")
    val messages: List<ChatMessageResponse>,
    @field:Schema(description = "더 오래된 메시지를 요청할 때 `cursor`로 보낼 값. 더 없으면 null", example = "221")
    val nextCursor: Long? = null,
)
