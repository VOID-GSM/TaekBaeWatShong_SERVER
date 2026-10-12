package com.example.taekbaewatshongserver.domain.chat.controller

import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatMessageSendRequest
import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatRoomCreateRequest
import com.example.taekbaewatshongserver.domain.chat.dto.response.*
import com.example.taekbaewatshongserver.domain.chat.service.ChatService
import com.example.taekbaewatshongserver.domain.user.entity.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
) {

    @GetMapping("/partners")
    fun getPartners(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ChatPartnerListResponse> {
        val response = chatService.getPartners(user, keyword, page, size)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/rooms")
    fun createRoom(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ChatRoomCreateRequest,
    ): ResponseEntity<ChatRoomResponse> {
        val result = chatService.createOrGetRoom(user, request)
        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(result.room)
    }

    @GetMapping("/rooms")
    fun getMyRooms(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<ChatRoomListResponse> {
        val response = chatService.getMyRooms(user)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/rooms/{roomId}")
    fun getRoom(
        @AuthenticationPrincipal user: User,
        @PathVariable roomId: Long,
    ): ResponseEntity<ChatRoomResponse> {
        val response = chatService.getRoom(user, roomId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/rooms/{roomId}/messages")
    fun getMessages(
        @AuthenticationPrincipal user: User,
        @PathVariable roomId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ChatMessageListResponse> {
        val response = chatService.getMessages(user, roomId, cursor, size)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/rooms/{roomId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal user: User,
        @PathVariable roomId: Long,
        @RequestBody @Valid request: ChatMessageSendRequest,
    ): ResponseEntity<ChatMessageResponse> {
        val response = chatService.sendMessage(user, roomId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PatchMapping("/rooms/{roomId}/read")
    fun markAsRead(
        @AuthenticationPrincipal user: User,
        @PathVariable roomId: Long,
    ): ResponseEntity<ChatMessageReadResponse> {
        val response = chatService.markAsRead(user, roomId)
        return ResponseEntity.ok(response)
    }
}
