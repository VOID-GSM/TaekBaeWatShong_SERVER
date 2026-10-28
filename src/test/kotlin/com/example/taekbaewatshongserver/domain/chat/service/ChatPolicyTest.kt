package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ChatException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatPolicyTest {

    private var sequence = 0L

    private fun user(role: Role, provider: AuthProvider = AuthProvider.DATAGSM): User = User(
        id = ++sequence,
        email = "user$sequence@gsm.hs.kr",
        name = "사용자$sequence",
        provider = provider,
        role = role,
    )

    @Test
    fun `택배기사와 학생은 채팅할 수 있다`() {
        val driver = user(Role.ADMIN, AuthProvider.LOCAL)
        val student = user(Role.STUDENT)

        assertEquals(ChatRoomType.DRIVER, ChatPolicy.resolveRoomType(driver, student))
        assertEquals(ChatRoomType.DRIVER, ChatPolicy.resolveRoomType(student, driver))
    }

    @Test
    fun `택배기사와 선생님은 채팅할 수 있다`() {
        val driver = user(Role.ADMIN, AuthProvider.LOCAL)
        val teacher = user(Role.TEACHER)

        assertEquals(ChatRoomType.DRIVER, ChatPolicy.resolveRoomType(driver, teacher))
        assertEquals(ChatRoomType.DRIVER, ChatPolicy.resolveRoomType(teacher, driver))
    }

    @Test
    fun `구글로 로그인한 학생도 택배기사와는 채팅할 수 있다`() {
        val driver = user(Role.ADMIN, AuthProvider.LOCAL)
        val student = user(Role.STUDENT, AuthProvider.GOOGLE)

        assertEquals(ChatRoomType.DRIVER, ChatPolicy.resolveRoomType(student, driver))
    }

    @Test
    fun `DataGSM으로 로그인한 학생과 선생님은 채팅할 수 있다`() {
        val student = user(Role.STUDENT)
        val teacher = user(Role.TEACHER)

        assertEquals(ChatRoomType.CLIENT, ChatPolicy.resolveRoomType(student, teacher))
        assertEquals(ChatRoomType.CLIENT, ChatPolicy.resolveRoomType(teacher, student))
    }

    @Test
    fun `한쪽이라도 DataGSM 로그인이 아니면 학생과 선생님은 채팅할 수 없다`() {
        val googleStudent = user(Role.STUDENT, AuthProvider.GOOGLE)
        val teacher = user(Role.TEACHER)

        val exception = assertFailsWith<ChatException.NotAllowed> {
            ChatPolicy.resolveRoomType(googleStudent, teacher)
        }
        assertTrue(exception.message?.contains("DataGSM") == true)
        assertFailsWith<ChatException.NotAllowed> { ChatPolicy.resolveRoomType(teacher, googleStudent) }
    }

    @Test
    fun `학생끼리는 채팅할 수 없다`() {
        assertFailsWith<ChatException.NotAllowed> {
            ChatPolicy.resolveRoomType(user(Role.STUDENT), user(Role.STUDENT))
        }
    }

    @Test
    fun `선생님끼리는 채팅할 수 없다`() {
        assertFailsWith<ChatException.NotAllowed> {
            ChatPolicy.resolveRoomType(user(Role.TEACHER), user(Role.TEACHER))
        }
    }

    @Test
    fun `택배기사끼리는 채팅할 수 없다`() {
        assertFailsWith<ChatException.NotAllowed> {
            ChatPolicy.resolveRoomType(user(Role.ADMIN, AuthProvider.LOCAL), user(Role.ADMIN, AuthProvider.LOCAL))
        }
    }

    @Test
    fun `자기 자신과는 채팅할 수 없다`() {
        val student = user(Role.STUDENT)

        assertFailsWith<ChatException.SelfChat> { ChatPolicy.resolveRoomType(student, student) }
    }

    @Test
    fun `isAllowed는 예외 대신 허용 여부를 돌려준다`() {
        val student = user(Role.STUDENT)

        assertTrue(ChatPolicy.isAllowed(student, user(Role.TEACHER)))
        assertFalse(ChatPolicy.isAllowed(student, user(Role.STUDENT)))
    }

    @Test
    fun `채팅 상대 조회 조건은 본인 역할과 로그인 수단에 따라 달라진다`() {
        assertEquals(
            ChatPartnerCondition(roles = setOf(Role.STUDENT, Role.TEACHER)),
            ChatPolicy.partnerCondition(user(Role.ADMIN, AuthProvider.LOCAL)),
        )
        assertEquals(
            ChatPartnerCondition(roles = setOf(Role.ADMIN), dataGsmOnlyRole = Role.TEACHER),
            ChatPolicy.partnerCondition(user(Role.STUDENT)),
        )
        assertEquals(
            ChatPartnerCondition(roles = setOf(Role.ADMIN)),
            ChatPolicy.partnerCondition(user(Role.TEACHER, AuthProvider.GOOGLE)),
        )
    }

    @Test
    fun `채팅 상대 조회 조건은 isAllowed와 같은 결과를 낸다`() {
        val users = Role.entries.flatMap { role -> AuthProvider.entries.map { provider -> user(role, provider) } }

        for (viewer in users) {
            val condition = ChatPolicy.partnerCondition(viewer)
            for (candidate in users.filter { it.id != viewer.id }) {
                assertEquals(
                    ChatPolicy.isAllowed(viewer, candidate),
                    condition.matches(candidate),
                    "${viewer.role}/${viewer.provider} -> ${candidate.role}/${candidate.provider}",
                )
            }
        }
    }
}
