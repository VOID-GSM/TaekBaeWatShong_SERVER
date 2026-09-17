package com.example.taekbaewatshongserver.domain.report.controller

import com.example.taekbaewatshongserver.domain.report.dto.request.ReportCreateRequest
import com.example.taekbaewatshongserver.domain.report.dto.request.ReportProgressUpdateRequest
import com.example.taekbaewatshongserver.domain.report.dto.response.*
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.report.service.ReportService
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/report")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping
    fun createReport(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ReportCreateRequest
    ): ResponseEntity<ReportCreateResponse> {
        val response = reportService.createReport(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/me")
    fun getMyReports(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<MyReportListResponse> {
        val response = reportService.getMyReports(user)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{reportId}")
    fun getReport(
        @AuthenticationPrincipal user: User,
        @PathVariable reportId: Long
    ): ResponseEntity<ReportDetailResponse> {
        val response = reportService.getReport(user, reportId)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAllReports(
        @RequestParam(required = false) progress: ReportProgress?
    ): ResponseEntity<ReportListResponse> {
        val response = reportService.getAllReports(progress)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{reportId}/progress")
    fun updateProgress(
        @PathVariable reportId: Long,
        @RequestBody request: ReportProgressUpdateRequest
    ): ResponseEntity<ReportProgressResponse> {
        val response = reportService.updateProgress(reportId, request)
        return ResponseEntity.ok(response)
    }
}
