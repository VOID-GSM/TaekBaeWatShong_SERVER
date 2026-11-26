package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class AdminInviteCodeException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class InvalidFormat(
        message: String = "초대 코드 형식이 올바르지 않습니다.",
    ) : AdminInviteCodeException(HttpStatus.BAD_REQUEST, message)

    class Invalid(
        message: String = "유효하지 않은 초대 코드입니다. 만료되었거나 사용 인원이 다 찬 코드일 수 있습니다.",
    ) : AdminInviteCodeException(HttpStatus.BAD_REQUEST, message)

    class InvalidMaxUses(
        message: String = "초대 인원은 1명 이상 20명 이하로 지정해주세요.",
    ) : AdminInviteCodeException(HttpStatus.BAD_REQUEST, message)
}
