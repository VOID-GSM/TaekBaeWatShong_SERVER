package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ChatException

/**
 * 1:1 채팅 상대 허용 규칙.
 *
 * - 택배기사(ADMIN) ↔ 학생/선생님: 항상 허용
 * - 학생 ↔ 선생님: 양쪽 모두 DataGSM 계정으로 로그인한 경우에만 허용
 * - 그 밖의 조합(택배기사끼리, 학생끼리, 선생님끼리)은 모두 불가
 */
object ChatPolicy {

    private val CLIENT_ROLES = setOf(Role.STUDENT, Role.TEACHER)

    fun resolveRoomType(user: User, partner: User): ChatRoomType {
        if (user.id != null && user.id == partner.id) {
            throw ChatException.SelfChat()
        }

        val userIsClient = user.role in CLIENT_ROLES
        val partnerIsClient = partner.role in CLIENT_ROLES

        return when {
            user.role == Role.ADMIN && partnerIsClient -> ChatRoomType.DRIVER
            userIsClient && partner.role == Role.ADMIN -> ChatRoomType.DRIVER
            userIsClient && partnerIsClient -> resolveClientRoomType(user, partner)
            else -> throw ChatException.NotAllowed()
        }
    }

    fun isAllowed(user: User, partner: User): Boolean = try {
        resolveRoomType(user, partner)
        true
    } catch (e: ChatException) {
        false
    }

    fun candidateRoles(user: User): Set<Role> = when (user.role) {
        Role.ADMIN -> CLIENT_ROLES
        else -> CLIENT_ROLES + Role.ADMIN
    }

    private fun resolveClientRoomType(user: User, partner: User): ChatRoomType {
        if (user.provider != AuthProvider.DATAGSM || partner.provider != AuthProvider.DATAGSM) {
            throw ChatException.NotAllowed("학생·선생님 간 1:1 채팅은 양쪽 모두 DataGSM 계정으로 로그인해야 가능합니다.")
        }
        if (user.role == partner.role) {
            throw ChatException.NotAllowed("학생·선생님 간 1:1 채팅은 학생과 선생님 사이에서만 가능합니다.")
        }
        return ChatRoomType.CLIENT
    }
}
