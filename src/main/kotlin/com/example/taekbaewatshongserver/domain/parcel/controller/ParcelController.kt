package com.example.taekbaewatshongserver.domain.parcel.controller

import com.example.taekbaewatshongserver.domain.parcel.dto.request.*
import com.example.taekbaewatshongserver.domain.parcel.dto.response.*
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import com.example.taekbaewatshongserver.domain.parcel.service.ParcelService
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

@Tag(name = ApiTags.PARCEL)
@RestController
@RequestMapping("/parcel")
class ParcelController(
    private val parcelService: ParcelService,
) {

    @Operation(
        summary = "택배 등록",
        description = """
            로그인한 사용자가 받을 택배를 운송장 번호로 등록한다. 등록된 택배는 PENDING(도착 예정) 상태로 시작한다.

            - 택배사·운송장 번호는 외부 배송 조회 API(APICK)로 검증한다. 외부 API 장애 시에는 검증을 건너뛰고 등록한다.
            - `alias`를 비우면 사용자 이름이 별칭으로 저장된다.
            - 등록하면 본인에게 `REGISTERED` 알림이 생성된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "등록 성공"),
        ApiResponse(
            responseCode = "400",
            description = "운송장 번호가 유효하지 않거나 배송 조회가 되지 않는 택배",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "유효하지 않은 운송장 번호이거나 조회할 수 없는 택배입니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 등록된 운송장 번호 (다른 사용자가 등록한 경우 포함)",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "이미 등록된 운송장 번호입니다."}""")],
                ),
            ],
        ),
    )
    @PostMapping
    fun registerParcel(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ParcelRegisterRequest,
    ): ResponseEntity<ParcelResponse> {
        val response = parcelService.registerParcel(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "전체 택배 조회 (관리자)",
        description = """
            모든 사용자의 택배를 등록 최신순으로 조회한다.

            - `status`를 생략하면 PENDING·ARRIVED 전체와 **최근 3일 이내에 수령한** CLAIMED 택배를 함께 반환한다.
            - `status=CLAIMED`도 최근 3일 이내 수령 건만 반환한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`status`가 허용되지 않은 값",
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
    fun getAllParcels(
        @Parameter(description = "택배 상태 필터 (생략 시 전체)", example = "ARRIVED")
        @RequestParam(required = false)
        status: ParcelStatus?,
    ): ResponseEntity<ParcelListResponse> {
        val response = parcelService.getAllParcels(status)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "내 택배 조회",
        description = """
            로그인한 사용자가 등록한 택배를 등록 최신순으로 조회한다.

            - `status`를 생략하면 PENDING·ARRIVED 전체와 **최근 3일 이내에 수령한** CLAIMED 택배를 함께 반환한다.
            - `status=CLAIMED`도 최근 3일 이내 수령 건만 반환한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`status`가 허용되지 않은 값",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @GetMapping("/me")
    fun getMyParcels(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "택배 상태 필터 (생략 시 전체)", example = "PENDING")
        @RequestParam(required = false)
        status: ParcelStatus?,
    ): ResponseEntity<ParcelListResponse> {
        val response = parcelService.getMyParcels(user, status)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "택배 도착 처리 (관리자)",
        description = """
            운송장 바코드를 스캔해 택배를 ARRIVED(도착 완료) 상태로 바꾸고 도착 시각을 기록한다.

            - PENDING 상태의 택배만 처리할 수 있다.
            - `zone`을 함께 보내면 보관 구역도 바로 배정한다. 생략하면 구역은 비어 있는 채로 남는다.
            - 처리하면 택배 주인에게 `ARRIVED` 알림이 생성된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "도착 처리 성공. 변경된 택배 정보를 반환한다."),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "해당 운송장 번호로 등록된 택배가 없음",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 운송장 번호의 택배를 찾을 수 없습니다. (123456789012)"}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "409",
            description = "PENDING 상태가 아님 (이미 도착 처리됐거나 수령 완료됨)",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "도착 대기(PENDING) 상태의 택배만 스캔 완료할 수 있습니다. (현재 상태: ARRIVED)"}""")],
                ),
            ],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/complete")
    fun completeParcelScan(
        @RequestBody request: ParcelCompleteRequest,
    ): ResponseEntity<ParcelResponse> {
        val response = parcelService.completeParcelScan(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "구역별 보관 택배 조회 (관리자)",
        description = """
            보관 중인(ARRIVED) 택배를 보관 구역별로 묶어 조회한다.

            - `zone`을 생략하면 A~F 구역과 **구역 미배정(`zone: null`)** 그룹을 모두 반환한다. 택배가 없는 구역도 `count: 0`으로 포함된다.
            - `zone`을 지정하면 해당 구역 그룹 하나만 반환한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`zone`이 허용되지 않은 값",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/zone")
    fun getParcelsByZone(
        @Parameter(description = "조회할 보관 구역 (생략 시 전체 구역 + 미배정)", example = "A")
        @RequestParam(required = false)
        zone: Zone?,
    ): ResponseEntity<ParcelZoneGroupResponse> {
        val response = parcelService.getParcelsByZone(zone)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "보관 구역 배정 (관리자)",
        description = """
            택배의 보관 구역을 배정하거나 다른 구역으로 옮긴다. 택배 상태와 관계없이 변경할 수 있다.

            - 택배가 ARRIVED 상태일 때만 택배 주인에게 `ZONE_ASSIGNED` 알림이 생성된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "배정 성공"),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 택배 ID",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "존재하지 않는 택배 ID입니다."}""")],
                ),
            ],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{parcelId}/zone")
    fun assignZone(
        @Parameter(description = "택배 ID", example = "1")
        @PathVariable
        parcelId: Long,
        @RequestBody request: ParcelZoneAssignRequest,
    ): ResponseEntity<ParcelZoneAssignResponse> {
        val response = parcelService.assignZone(parcelId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "택배 수령 처리",
        description = """
            택배 주인이 보관함에서 택배를 가져갔음을 처리한다. 상태가 CLAIMED(수령 완료)로 바뀌고 수령 시각이 기록된다.

            - 본인이 등록한 택배만 처리할 수 있다.
            - ARRIVED 상태의 택배만 처리할 수 있다.
            - 처리하면 본인에게 `CLAIMED` 알림이 생성된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수령 처리 성공"),
        ApiResponse(
            responseCode = "403",
            description = "본인의 택배가 아님",
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
            description = "ARRIVED 상태가 아님 (아직 도착 전이거나 이미 수령함)",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "도착 완료(ARRIVED) 상태의 택배만 수령 처리할 수 있습니다. (현재 상태: CLAIMED)"}""")],
                ),
            ],
        ),
    )
    @PatchMapping("/claim")
    fun claimParcel(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ParcelClaimRequest,
    ): ResponseEntity<ParcelClaimResponse> {
        val response = parcelService.claimParcel(user, request)
        return ResponseEntity.ok(response)
    }
}
