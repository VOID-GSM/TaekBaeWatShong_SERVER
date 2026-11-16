package com.example.taekbaewatshongserver.domain.chat.controller

import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatMessageSendRequest
import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatRoomCreateRequest
import com.example.taekbaewatshongserver.domain.chat.dto.response.*
import com.example.taekbaewatshongserver.domain.chat.service.ChatService
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = ApiTags.CHAT)
@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
) {

    @Operation(
        summary = "채팅 상대 검색",
        description = """
            로그인한 사용자가 1:1 채팅을 시작할 수 있는 상대를 이름순으로 조회한다. 본인은 제외된다.

            - 택배기사(ADMIN): 모든 학생·선생님
            - 학생·선생님: 모든 택배기사(ADMIN). 본인이 DataGSM 계정이면 DataGSM 계정인 선생님(학생이면)·학생(선생님이면)도 포함
            - `keyword`는 이름 또는 학번에 포함된 문자열로 검색한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    @GetMapping("/partners")
    fun getPartners(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "이름 또는 학번 검색어 (생략 시 전체)", example = "홍길")
        @RequestParam(required = false)
        keyword: String?,
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(required = false)
        page: Int?,
        @Parameter(description = "페이지 크기 (1~100, 기본 30). 범위를 벗어나면 가까운 값으로 맞춰진다.", example = "30")
        @RequestParam(required = false)
        size: Int?,
    ): ResponseEntity<ChatPartnerListResponse> {
        val response = chatService.getPartners(user, keyword, page, size)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "1:1 채팅방 생성",
        description = """
            상대와의 1:1 채팅방을 만든다. 두 사람 사이의 채팅방은 하나뿐이므로, 이미 있으면 새로 만들지 않고 기존 방을 반환한다.

            - 새로 만들면 201, 기존 방을 반환하면 200으로 응답한다. 응답 본문 형식은 같다.
            - 채팅 가능한 상대 규칙은 `채팅 상대 검색` API와 같다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "채팅방을 새로 만듦"),
        ApiResponse(responseCode = "200", description = "이미 있던 채팅방을 반환함"),
        ApiResponse(
            responseCode = "400",
            description = "자기 자신과 채팅하려 함",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "자신과는 1:1 채팅을 할 수 없습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "채팅할 수 없는 상대",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(name = "채팅 불가 상대", value = """{"message": "해당 상대와는 1:1 채팅을 할 수 없습니다."}"""),
                        ExampleObject(
                            name = "DataGSM 계정 아님",
                            value = """{"message": "학생·선생님 간 1:1 채팅은 양쪽 모두 DataGSM 계정으로 로그인해야 가능합니다."}""",
                        ),
                    ],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 상대",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "채팅 상대를 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PostMapping("/rooms")
    fun createRoom(
        @AuthenticationPrincipal user: User,
        @RequestBody request: ChatRoomCreateRequest,
    ): ResponseEntity<ChatRoomResponse> {
        val result = chatService.createOrGetRoom(user, request)
        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(result.room)
    }

    @Operation(
        summary = "내 채팅방 목록 조회",
        description = """
            로그인한 사용자가 참여한 채팅방을 최근 메시지 순으로 조회한다. 메시지가 없는 방은 생성 시각 기준으로 정렬된다.

            - `totalUnreadCount`는 모든 방의 안 읽은 메시지 수 합계다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    @GetMapping("/rooms")
    fun getMyRooms(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<ChatRoomListResponse> {
        val response = chatService.getMyRooms(user)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "채팅방 조회",
        description = "참여 중인 채팅방 한 개의 정보와 안 읽은 메시지 수를 조회한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "403",
            description = "참여하지 않은 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "본인이 참여한 채팅방만 이용할 수 있습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 채팅방을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @GetMapping("/rooms/{roomId}")
    fun getRoom(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "채팅방 ID", example = "1")
        @PathVariable
        roomId: Long,
    ): ResponseEntity<ChatRoomResponse> {
        val response = chatService.getRoom(user, roomId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "메시지 목록 조회",
        description = """
            채팅방의 메시지를 **최신순**으로 조회한다. 커서 기반 페이지네이션을 쓴다.

            - 첫 페이지는 `cursor` 없이 요청하고, 이전(더 오래된) 메시지는 응답의 `nextCursor` 값을 `cursor`로 보내 이어서 받는다.
            - `nextCursor`가 `null`이면 가장 오래된 메시지까지 받은 것이다.
            - 조회만으로는 읽음 처리되지 않는다. `메시지 읽음 처리` API를 따로 호출해야 한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "403",
            description = "참여하지 않은 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "본인이 참여한 채팅방만 이용할 수 있습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 채팅방을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @GetMapping("/rooms/{roomId}/messages")
    fun getMessages(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "채팅방 ID", example = "1")
        @PathVariable
        roomId: Long,
        @Parameter(description = "이전 페이지 응답의 `nextCursor` (첫 페이지는 생략). 이 ID보다 오래된 메시지를 조회한다.", example = "250")
        @RequestParam(required = false)
        cursor: Long?,
        @Parameter(description = "페이지 크기 (1~100, 기본 30). 범위를 벗어나면 가까운 값으로 맞춰진다.", example = "30")
        @RequestParam(required = false)
        size: Int?,
    ): ResponseEntity<ChatMessageListResponse> {
        val response = chatService.getMessages(user, roomId, cursor, size)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "메시지 전송",
        description = """
            채팅방에 메시지를 보낸다.

            - 메시지 앞뒤 공백은 제거되어 저장된다.
            - 보내는 시점에도 채팅 가능한 상대인지 다시 확인한다. (예: 상대의 계정 정보가 바뀌어 더는 채팅할 수 없는 경우 403)
            - 채팅방 목록의 마지막 메시지 미리보기는 앞 200자까지만 저장된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "전송 성공"),
        ApiResponse(
            responseCode = "400",
            description = "메시지가 비어 있거나 1000자를 넘음",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(name = "빈 메시지", value = """{"message": "메시지 내용을 입력해주세요."}"""),
                        ExampleObject(name = "1000자 초과", value = """{"message": "메시지는 1000자까지 입력할 수 있습니다."}"""),
                    ],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "참여하지 않은 채팅방이거나 더는 채팅할 수 없는 상대",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "본인이 참여한 채팅방만 이용할 수 있습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 채팅방을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PostMapping("/rooms/{roomId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "채팅방 ID", example = "1")
        @PathVariable
        roomId: Long,
        @RequestBody @Valid request: ChatMessageSendRequest,
    ): ResponseEntity<ChatMessageResponse> {
        val response = chatService.sendMessage(user, roomId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "메시지 읽음 처리",
        description = """
            채팅방에서 상대가 보낸 안 읽은 메시지를 모두 읽음 처리한다. 채팅방에 들어가거나 새 메시지를 받았을 때 호출한다.

            - `readCount`는 이번에 읽음 처리된 메시지 수다. 처리 후 이 방의 안 읽은 메시지 수(`unreadCount`)는 항상 0이다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        ApiResponse(
            responseCode = "403",
            description = "참여하지 않은 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "본인이 참여한 채팅방만 이용할 수 있습니다."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 채팅방",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 채팅방을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PatchMapping("/rooms/{roomId}/read")
    fun markAsRead(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "채팅방 ID", example = "1")
        @PathVariable
        roomId: Long,
    ): ResponseEntity<ChatMessageReadResponse> {
        val response = chatService.markAsRead(user, roomId)
        return ResponseEntity.ok(response)
    }
}
