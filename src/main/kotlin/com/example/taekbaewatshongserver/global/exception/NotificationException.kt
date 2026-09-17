package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class NotificationException(
    val status: HttpStatus,
    message: String
) : RuntimeException(message) {

    class NotFound(
        message: String = "해당 알림을 찾을 수 없습니다."
    ) : NotificationException(HttpStatus.NOT_FOUND, message)
}
