package com.example.taekbaewatshongserver.domain.report.dto.request

import java.time.LocalDateTime

data class ReportCreateRequest(
    val invoiceNumber: String,
    val lostEstimatedAt: LocalDateTime,
    val content: String
)
