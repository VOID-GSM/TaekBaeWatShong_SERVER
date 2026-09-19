package com.example.taekbaewatshongserver.domain.notification.controller

import com.example.taekbaewatshongserver.domain.notification.dto.request.DeviceTokenRegisterRequest
import com.example.taekbaewatshongserver.domain.notification.dto.response.DeviceTokenResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationListResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationReadResponse
import com.example.taekbaewatshongserver.domain.notification.service.DeviceTokenService
import com.example.taekbaewatshongserver.domain.notification.service.NotificationService
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/notification")
class NotificationController(
    private val notificationService: NotificationService,
    private val deviceTokenService: DeviceTokenService
) {

    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<NotificationListResponse> {
        val response = notificationService.getNotifications(user, cursor, size)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @AuthenticationPrincipal user: User,
        @PathVariable notificationId: Long
    ): ResponseEntity<NotificationReadResponse> {
        val response = notificationService.markAsRead(user, notificationId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/token")
    fun registerDeviceToken(
        @AuthenticationPrincipal user: User,
        @RequestBody request: DeviceTokenRegisterRequest
    ): ResponseEntity<DeviceTokenResponse> {
        val response = deviceTokenService.registerDeviceToken(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
