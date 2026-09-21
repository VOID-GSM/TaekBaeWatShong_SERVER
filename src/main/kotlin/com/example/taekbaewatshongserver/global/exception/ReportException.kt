package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus

sealed class ReportException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message) {

    class NotFound(
        message: String = "해당 분실 신고를 찾을 수 없습니다.",
    ) : ReportException(HttpStatus.NOT_FOUND, message)

    class ParcelNotFound(
        message: String = "해당 운송장 번호의 택배를 찾을 수 없습니다.",
    ) : ReportException(HttpStatus.NOT_FOUND, message)

    class Conflict(
        message: String = "이미 분실 신고된 택배입니다.",
    ) : ReportException(HttpStatus.CONFLICT, message)
}
