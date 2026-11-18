package com.example.taekbaewatshongserver.domain.user.controller

import com.example.taekbaewatshongserver.domain.user.dto.UserResponse
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = ApiTags.USER)
@RestController
@RequestMapping("/api/users")
class UserController {

    @Operation(
        summary = "내 정보 조회",
        description = "토큰의 사용자 정보를 조회한다. 로그인 직후 역할(`role`)을 확인해 화면을 나눌 때 쓴다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: User): UserResponse = UserResponse.from(user)
}
