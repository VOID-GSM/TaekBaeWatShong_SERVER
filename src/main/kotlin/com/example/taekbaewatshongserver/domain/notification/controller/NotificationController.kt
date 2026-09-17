package com.example.taekbaewatshongserver.domain.notification.controller

import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationListResponse
import com.example.taekbaewatshongserver.domain.notification.service.NotificationService
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notification")
class NotificationController(
    private val notificationService: NotificationService
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
}
