package com.example.taekbaewatshongserver.global.exception

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "비즈니스 에러 응답")
data class ErrorResponse(
    @field:Schema(description = "에러 사유", example = "해당 택배를 찾을 수 없습니다.")
    val message: String,
)

/**
 * Spring Boot 기본 에러 응답(`/error`) 형식. 문서화 용도로만 쓴다.
 *
 * 인증·권한 실패, ResponseStatusException, 잘못된 JSON/enum 값 등은 이 형식으로 응답된다.
 */
@Schema(description = "Spring 기본 에러 응답 (에러 사유 문구는 담기지 않음)")
data class DefaultErrorResponse(
    @field:Schema(description = "에러 발생 시각", example = "2026-11-09T05:30:00.000+00:00")
    val timestamp: String,
    @field:Schema(description = "HTTP 상태 코드", example = "403")
    val status: Int,
    @field:Schema(description = "HTTP 상태 이름", example = "Forbidden")
    val error: String,
    @field:Schema(description = "요청 경로", example = "/parcel")
    val path: String,
)
