package com.example.taekbaewatshongserver.global.security

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminBootstrapTest {

    @Test
    fun `설정값과 입력값 모두 대소문자와 앞뒤 공백을 무시하고 비교한다`() {
        val bootstrap = AdminBootstrap(" TeamVoid0107@Gmail.com ")

        assertTrue(bootstrap.isBootstrapEmail("teamvoid0107@gmail.com"))
        assertTrue(bootstrap.isBootstrapEmail("  TEAMVOID0107@GMAIL.COM "))
        assertFalse(bootstrap.isBootstrapEmail("other@gmail.com"))
    }

    @Test
    fun `설정값이 비어 있으면 어떤 이메일도 부트스트랩으로 보지 않는다`() {
        val bootstrap = AdminBootstrap("  ")

        assertFalse(bootstrap.isBootstrapEmail(""))
        assertFalse(bootstrap.isBootstrapEmail("  "))
    }
}
