package com.example.taekbaewatshongserver.domain.chat.entity

enum class ChatRoomType {
    DRIVER, // 택배기사(ADMIN) ↔ 학생/선생님
    CLIENT, // 학생 ↔ 선생님 (DataGSM 로그인 전용)
}
