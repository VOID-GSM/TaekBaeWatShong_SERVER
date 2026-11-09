package com.example.taekbaewatshongserver.domain.chat.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "채팅 상대 목록 (페이지)")
data class ChatPartnerListResponse(
    @field:Schema(description = "채팅 상대 목록 (이름순)")
    val partners: List<ChatPartnerResponse>,
    @field:Schema(description = "다음 페이지가 있는지 여부", example = "false")
    val hasNext: Boolean,
)
