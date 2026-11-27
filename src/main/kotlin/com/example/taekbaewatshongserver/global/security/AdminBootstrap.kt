package com.example.taekbaewatshongserver.global.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AdminBootstrap(
    @Value("\${app.admin.bootstrap-email}") email: String,
) {
    private val email = email.trim().lowercase()

    fun isBootstrapEmail(candidate: String): Boolean = email.isNotEmpty() && candidate.trim().lowercase() == email
}
