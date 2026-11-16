package com.example.taekbaewatshongserver.domain.driver.controller

import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrCreateRequest
import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrScanRequest
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrCreateResponse
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrScanResponse
import com.example.taekbaewatshongserver.domain.driver.service.DriverService
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = ApiTags.DRIVER_QR)
@RestController
@RequestMapping("/qr")
class DriverController(
    private val driverService: DriverService,
) {

    @Operation(
        summary = "택배기사 QR 발급 (관리자)",
        description = """
            택배기사 정보를 등록하고 그 기사를 식별하는 QR 코드를 발급한다.

            - 호출할 때마다 새 기사와 새 QR 토큰(UUID)이 만들어진다. 같은 이름으로 다시 호출해도 기존 QR을 돌려주지 않는다.
            - `qrImageUrl`은 300×300 PNG 이미지의 data URL이므로 `<img src>`에 그대로 넣으면 된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "발급 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`driverName` 또는 `deliveryCompany`가 비어 있음. 메시지는 Bean Validation 기본 문구다.",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "must not be blank"}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createQr(
        @Valid @RequestBody request: DriverQrCreateRequest,
    ): ResponseEntity<DriverQrCreateResponse> {
        val response = driverService.createQr(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "택배기사 QR 스캔 (관리자)",
        description = """
            택배기사의 QR 코드를 스캔해 어느 기사가 언제 왔는지 기록한다.

            - 같은 QR을 여러 번 스캔하면 스캔할 때마다 기록이 따로 남는다.
            - 스캔한 관리자 계정도 함께 기록된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "스캔 기록 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`qrToken`이 비어 있음. 메시지는 Bean Validation 기본 문구다.",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "must not be blank"}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "발급된 적 없는 QR 토큰",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "유효하지 않은 QR 토큰입니다."}""")],
                ),
            ],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scan")
    fun scan(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: DriverQrScanRequest,
    ): ResponseEntity<DriverQrScanResponse> {
        val response = driverService.scan(user, request)
        return ResponseEntity.ok(response)
    }
}
