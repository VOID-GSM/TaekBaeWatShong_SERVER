package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

sealed class ParcelException(
    val status: HttpStatus,
    message: String
) : RuntimeException(message) {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    class NotFound(
        message: String = "해당 택배를 찾을 수 없습니다."
    ) : ParcelException(HttpStatus.NOT_FOUND, message)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class InvalidInvoice(
        message: String = "유효하지 않은 운송장 번호입니다."
    ) : ParcelException(HttpStatus.BAD_REQUEST, message)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class DuplicateInvoice(
        message: String = "이미 등록되었거나 처리 중인 운송장 번호입니다."
    ) : ParcelException(HttpStatus.BAD_REQUEST, message)

    @ResponseStatus(HttpStatus.CONFLICT)
    class Conflict(
        message: String = "이미 존재하는 운송장 번호입니다."
    ) : ParcelException(HttpStatus.CONFLICT, message)
}