package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class ParcelException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class NotFound(
        message: String = "해당 택배를 찾을 수 없습니다.",
    ) : ParcelException(HttpStatus.NOT_FOUND, message)

    class InvalidInvoice(
        message: String = "유효하지 않은 운송장 번호입니다.",
    ) : ParcelException(HttpStatus.BAD_REQUEST, message)

    class Conflict(
        message: String = "이미 존재하는 운송장 번호입니다.",
    ) : ParcelException(HttpStatus.CONFLICT, message)

    class InvalidStatus(
        message: String = "잘못된 택배 상태 변경 요청입니다.",
    ) : ParcelException(HttpStatus.CONFLICT, message)
}
