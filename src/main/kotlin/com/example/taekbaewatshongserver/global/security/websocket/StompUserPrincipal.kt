package com.example.taekbaewatshongserver.global.security.websocket

import java.security.Principal

/** STOMP 세션의 사용자. 이름이 사용자 ID라서 `convertAndSendToUser(userId.toString(), ...)`로 보낼 수 있다. */
class StompUserPrincipal(
    val userId: Long,
) : Principal {
    override fun getName(): String = userId.toString()
}
