package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class NotificationException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class NotFound(
        message: String = "해당 알림을 찾을 수 없습니다.",
    ) : NotificationException(HttpStatus.NOT_FOUND, message)

    class InvalidPlatform(
        message: String = "유효하지 않은 플랫폼입니다.",
    ) : NotificationException(HttpStatus.BAD_REQUEST, message)

    class Conflict(
        message: String = "이미 등록 처리 중인 디바이스 토큰입니다. 다시 시도해주세요.",
    ) : NotificationException(HttpStatus.CONFLICT, message)
}
