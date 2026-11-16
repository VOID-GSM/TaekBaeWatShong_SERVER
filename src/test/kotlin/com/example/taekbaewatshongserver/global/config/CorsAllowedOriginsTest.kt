package com.example.taekbaewatshongserver.global.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CorsAllowedOriginsTest {

    @Test
    fun `쉼표 앞뒤 공백과 빈 항목을 제거한다`() {
        val origins = CorsAllowedOrigins(" http://a.com, http://b.com ,,")

        assertEquals(listOf("http://a.com", "http://b.com"), origins.values)
    }
}
