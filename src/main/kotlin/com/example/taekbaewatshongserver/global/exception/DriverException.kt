package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class DriverException(
    val status: HttpStatus,
    message: String
) : RuntimeException(message) {

    class NotFound(
        message: String = "유효하지 않은 QR 토큰입니다."
    ) : DriverException(HttpStatus.NOT_FOUND, message)
}
