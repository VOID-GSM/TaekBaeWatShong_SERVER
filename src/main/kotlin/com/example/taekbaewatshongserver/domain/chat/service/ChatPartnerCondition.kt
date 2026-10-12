package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User

/**
 * 채팅 상대 후보 조건.
 *
 * @property roles 로그인 수단과 무관하게 채팅할 수 있는 역할
 * @property dataGsmOnlyRole DataGSM 계정으로 로그인한 경우에만 채팅할 수 있는 역할 (없으면 null)
 */
data class ChatPartnerCondition(
    val roles: Set<Role>,
    val dataGsmOnlyRole: Role? = null,
) {
    fun matches(candidate: User): Boolean = candidate.role in roles ||
        (candidate.role == dataGsmOnlyRole && candidate.provider == AuthProvider.DATAGSM)
}
