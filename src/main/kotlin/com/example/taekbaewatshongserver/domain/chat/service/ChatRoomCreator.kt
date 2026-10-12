package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 채팅방 insert를 호출자와 분리된 트랜잭션(별도 영속성 컨텍스트)에서 수행한다.
 *
 * 동시 생성으로 유니크 제약을 위반하더라도 이 트랜잭션만 롤백되므로,
 * 호출자 트랜잭션은 rollback-only로 표시되지 않고 기존 방을 다시 조회할 수 있다.
 */
@Component
class ChatRoomCreator(
    private val chatRoomRepository: ChatRoomRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(type: ChatRoomType, user: User, partner: User): ChatRoom = chatRoomRepository.saveAndFlush(ChatRoom.of(type, user, partner))
}
