package com.example.taekbaewatshongserver.domain.chat.repository

interface RoomUnreadCount {
    val roomId: Long
    val unreadCount: Long
}
