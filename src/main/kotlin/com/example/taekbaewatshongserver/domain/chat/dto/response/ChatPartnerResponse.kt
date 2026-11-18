package com.example.taekbaewatshongserver.domain.chat.dto.response

import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "채팅 상대")
data class ChatPartnerResponse(
    @field:Schema(description = "사용자 ID", example = "2")
    val id: Long,
    @field:Schema(description = "이름", example = "김택배")
    val name: String,
    @field:Schema(description = "역할. STUDENT(학생) / TEACHER(선생님) / ADMIN(택배기사)", example = "ADMIN")
    val role: Role,
    @field:Schema(description = "학번 (학생이 아니거나 없으면 null)")
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
