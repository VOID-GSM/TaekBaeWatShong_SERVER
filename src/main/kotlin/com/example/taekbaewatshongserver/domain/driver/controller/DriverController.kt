package com.example.taekbaewatshongserver.domain.driver.controller

import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrCreateRequest
import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrScanRequest
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrCreateResponse
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrScanResponse
import com.example.taekbaewatshongserver.domain.driver.service.DriverService
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/qr")
class DriverController(
    private val driverService: DriverService
) {

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createQr(
        @RequestBody request: DriverQrCreateRequest
    ): ResponseEntity<DriverQrCreateResponse> {
        val response = driverService.createQr(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scan")
    fun scan(
        @AuthenticationPrincipal user: User,
        @RequestBody request: DriverQrScanRequest
    ): ResponseEntity<DriverQrScanResponse> {
        val response = driverService.scan(user, request)
        return ResponseEntity.ok(response)
    }
}
