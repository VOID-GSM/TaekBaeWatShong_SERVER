package com.example.taekbaewatshongserver.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "요청 값이 올바르지 않습니다."
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to message))
    }

    @ExceptionHandler(ParcelException::class)
    fun handleParcelException(e: ParcelException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))

    @ExceptionHandler(DriverException::class)
    fun handleDriverException(e: DriverException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))

    @ExceptionHandler(ReportException::class)
    fun handleReportException(e: ReportException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))

    @ExceptionHandler(NotificationException::class)
    fun handleNotificationException(e: NotificationException): ResponseEntity<Map<String, String>> = ResponseEntity.status(e.status)
        .body(mapOf("message" to (e.message ?: "알 수 없는 오류가 발생했습니다.")))
}
