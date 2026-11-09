package com.example.taekbaewatshongserver.domain.report.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "분실 신고 접수 요청")
data class ReportCreateRequest(
    @field:Schema(description = "분실된 택배의 운송장 번호. 본인이 등록한 택배여야 한다.", example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    val invoiceNumber: String,
    @field:Schema(description = "분실 추정 시각 (`yyyy-MM-ddTHH:mm:ss`)", example = "2026-11-08T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    val lostEstimatedAt: LocalDateTime,
    @field:Schema(description = "분실 상황 설명", example = "보관함에 택배가 없습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,
)
