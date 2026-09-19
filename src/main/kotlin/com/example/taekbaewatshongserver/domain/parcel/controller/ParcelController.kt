package com.example.taekbaewatshongserver.domain.parcel.controller

import com.example.taekbaewatshongserver.domain.parcel.dto.request.*
import com.example.taekbaewatshongserver.domain.parcel.dto.response.*
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import com.example.taekbaewatshongserver.domain.parcel.service.ParcelService
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/parcel")
class ParcelController(
    private val parcelService: ParcelService
) {

    @PostMapping
    fun registerParcel(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ParcelRegisterRequest
    ): ResponseEntity<ParcelResponse> {
        val response = parcelService.registerParcel(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAllParcels(
        @RequestParam(required = false) status: ParcelStatus?
    ): ResponseEntity<ParcelListResponse> {
        val response = parcelService.getAllParcels(status)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getMyParcels(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) status: ParcelStatus?
    ): ResponseEntity<ParcelListResponse> {
        val response = parcelService.getMyParcels(user, status)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/complete")
    fun completeParcelScan(
        @RequestBody request: ParcelCompleteRequest
    ): ResponseEntity<ParcelResponse> {
        val response = parcelService.completeParcelScan(request)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/zone")
    fun getParcelsByZone(
        @RequestParam(required = false) zone: Zone?
    ): ResponseEntity<ParcelZoneGroupResponse> {
        val response = parcelService.getParcelsByZone(zone)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{parcelId}/zone")
    fun assignZone(
        @PathVariable parcelId: Long,
        @RequestBody request: ParcelZoneAssignRequest
    ): ResponseEntity<ParcelZoneAssignResponse> {
        val response = parcelService.assignZone(parcelId, request)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/claim")
    fun claimParcel(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ParcelClaimRequest
    ): ResponseEntity<ParcelClaimResponse> {
        val response = parcelService.claimParcel(user, request)
        return ResponseEntity.ok(response)
    }
}