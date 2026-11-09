package com.example.taekbaewatshongserver.domain.chat.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "메시지 전송 요청")
data class ChatMessageSendRequest(
    @field:Schema(description = "메시지 내용 (공백만은 불가, 최대 1000자). 앞뒤 공백은 제거되어 저장된다.", example = "택배 어디에 두셨나요?", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000)
    @field:NotBlank(message = "메시지 내용을 입력해주세요.")
    @field:Size(max = 1000, message = "메시지는 1000자까지 입력할 수 있습니다.")
    val content: String,
)
