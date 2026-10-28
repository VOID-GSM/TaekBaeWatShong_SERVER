package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User

data class ChatPartnerResponse(
    val id: Long,
    val name: String,
    val role: Role,
    val studentNumber: String? = null,
) {
    companion object {
        fun from(user: User): ChatPartnerResponse = ChatPartnerResponse(
            id = requireNotNull(user.id),
            name = user.name,
            role = user.role,
            studentNumber = user.studentNumber,
        )
    }
}
