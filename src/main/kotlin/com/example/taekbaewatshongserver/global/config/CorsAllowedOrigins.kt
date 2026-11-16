package com.example.taekbaewatshongserver.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** `app.cors.allowed-origins`(쉼표 구분)를 한 번만 파싱해 REST CORS와 웹소켓이 같은 목록을 쓰게 한다. */
@Component
class CorsAllowedOrigins(
    @Value("\${app.cors.allowed-origins}") raw: String,
) {
    val values: List<String> = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
