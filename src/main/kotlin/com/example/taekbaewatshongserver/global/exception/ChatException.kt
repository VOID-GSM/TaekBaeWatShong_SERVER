package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class ChatException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class RoomNotFound(
        message: String = "해당 채팅방을 찾을 수 없습니다.",
    ) : ChatException(HttpStatus.NOT_FOUND, message)

    class PartnerNotFound(
        message: String = "채팅 상대를 찾을 수 없습니다.",
    ) : ChatException(HttpStatus.NOT_FOUND, message)

    class NotParticipant(
        message: String = "본인이 참여한 채팅방만 이용할 수 있습니다.",
    ) : ChatException(HttpStatus.FORBIDDEN, message)

    class SelfChat(
        message: String = "자신과는 1:1 채팅을 할 수 없습니다.",
    ) : ChatException(HttpStatus.BAD_REQUEST, message)

    class NotAllowed(
        message: String = "해당 상대와는 1:1 채팅을 할 수 없습니다.",
    ) : ChatException(HttpStatus.FORBIDDEN, message)
}
