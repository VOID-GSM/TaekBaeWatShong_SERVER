package com.example.taekbaewatshongserver.domain.chat.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "1:1 채팅방 생성 요청")
data class ChatRoomCreateRequest(
    @field:Schema(description = "채팅 상대의 사용자 ID (`채팅 상대 검색` API의 `id`)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    val partnerId: Long,
)
