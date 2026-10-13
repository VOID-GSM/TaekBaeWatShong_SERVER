package com.example.taekbaewatshongserver.domain.chat.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChatMessageSendRequest(
    @field:NotBlank(message = "메시지 내용을 입력해주세요.")
    @field:Size(max = 1000, message = "메시지는 1000자까지 입력할 수 있습니다.")
    val content: String,
)
