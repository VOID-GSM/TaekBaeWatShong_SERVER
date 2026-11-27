package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class RefreshTokenException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class Invalid(
        message: String = "유효하지 않거나 만료된 refresh token입니다. 다시 로그인해주세요.",
    ) : RefreshTokenException(HttpStatus.UNAUTHORIZED, message)

    class ReuseDetected(
        message: String = "이미 사용된 refresh token입니다. 보안을 위해 다시 로그인해주세요.",
    ) : RefreshTokenException(HttpStatus.UNAUTHORIZED, message)
}
