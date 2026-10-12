package com.example.taekbaewatshongserver.domain.chat.dto.response

data class ChatPartnerListResponse(
    val partners: List<ChatPartnerResponse>,
    val hasNext: Boolean,
)
