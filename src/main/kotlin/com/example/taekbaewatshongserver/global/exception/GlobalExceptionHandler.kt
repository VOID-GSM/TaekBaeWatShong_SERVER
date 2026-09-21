package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ParcelException::class)
    fun handleParcelException(e: ParcelException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))

    @ExceptionHandler(ReportException::class)
    fun handleReportException(e: ReportException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))
}
