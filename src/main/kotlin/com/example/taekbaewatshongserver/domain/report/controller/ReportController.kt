package com.example.taekbaewatshongserver.domain.report.controller

import com.example.taekbaewatshongserver.domain.report.dto.request.ReportCreateRequest
import com.example.taekbaewatshongserver.domain.report.dto.request.ReportProgressUpdateRequest
import com.example.taekbaewatshongserver.domain.report.dto.response.*
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.report.service.ReportService
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = ApiTags.REPORT)
@RestController
@RequestMapping("/report")
class ReportController(
    private val reportService: ReportService,
) {

    @Operation(
        summary = "분실 신고 접수",
        description = """
            본인이 등록한 택배에 대해 분실 신고를 접수한다. 접수된 신고는 RECEIVED(접수) 상태로 시작한다.

            - 택배 한 건당 분실 신고는 하나만 할 수 있다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "접수 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`lostEstimatedAt` 날짜 형식이 잘못되는 등 요청 본문을 읽을 수 없음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "본인이 등록한 택배가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "해당 운송장 번호로 등록된 택배가 없음",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 운송장 번호의 택배를 찾을 수 없습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 분실 신고된 택배",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "이미 분실 신고된 택배입니다."}""")],
                ),
            ],
        ),
    )
    @PostMapping
    fun createReport(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ReportCreateRequest,
    ): ResponseEntity<ReportCreateResponse> {
        val response = reportService.createReport(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "내 분실 신고 목록 조회",
        description = "로그인한 사용자가 접수한 분실 신고를 접수 최신순으로 조회한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    @GetMapping("/me")
    fun getMyReports(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MyReportListResponse> {
        val response = reportService.getMyReports(user)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "분실 신고 상세 조회",
        description = """
            분실 신고 한 건을 조회한다.

            - 신고자 본인 또는 관리자(ADMIN)만 조회할 수 있다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "403",
            description = "본인의 신고가 아님 (관리자 제외)",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 분실 신고",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 분실 신고를 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @GetMapping("/{reportId}")
    fun getReport(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "분실 신고 ID", example = "1")
        @PathVariable
        reportId: Long,
    ): ResponseEntity<ReportDetailResponse> {
        val response = reportService.getReport(user, reportId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "전체 분실 신고 조회 (관리자)",
        description = "모든 사용자의 분실 신고를 접수 최신순으로 조회한다. `progress`로 진행 상태를 거를 수 있다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`progress`가 허용되지 않은 값",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAllReports(
        @Parameter(description = "진행 상태 필터 (생략 시 전체)", example = "RECEIVED")
        @RequestParam(required = false)
        progress: ReportProgress?,
    ): ResponseEntity<ReportListResponse> {
        val response = reportService.getAllReports(progress)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "분실 신고 진행 상태 변경 (관리자)",
        description = """
            분실 신고의 진행 상태를 바꾼다. 상태 순서에 제한은 없어 DONE에서 다시 IN_PROGRESS로 되돌릴 수도 있다.

            - 변경 시각이 `updatedAt`에 기록된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "변경 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`progress`가 허용되지 않은 값",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 분실 신고",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 분실 신고를 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{reportId}/progress")
    fun updateProgress(
        @Parameter(description = "분실 신고 ID", example = "1")
        @PathVariable
        reportId: Long,
        @RequestBody request: ReportProgressUpdateRequest,
    ): ResponseEntity<ReportProgressResponse> {
        val response = reportService.updateProgress(reportId, request)
        return ResponseEntity.ok(response)
    }
}
