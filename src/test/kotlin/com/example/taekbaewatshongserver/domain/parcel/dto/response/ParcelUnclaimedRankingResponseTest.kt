package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParcelUnclaimedRankingResponseTest {

    private var sequence = 0L

    private fun arrivedParcel(ownerName: String, alias: String, zone: Zone?, arrivedAt: LocalDateTime?): Parcel {
        val id = ++sequence
        val owner = User(
            id = id,
            email = "user$id@gsm.hs.kr",
            name = ownerName,
            provider = AuthProvider.DATAGSM,
            role = Role.STUDENT,
        )
        return Parcel(
            id = id,
            deliveryCompany = "CJ대한통운",
            invoiceNumber = "INV$id",
            alias = alias,
            owner = owner,
            zone = zone,
            status = ParcelStatus.ARRIVED,
            arrivedAt = arrivedAt,
        )
    }

    @Test
    fun `조회된 순서대로 1위부터 순위를 매긴다`() {
        val first = LocalDateTime.of(2026, 11, 15, 4, 48)
        val second = LocalDateTime.of(2026, 11, 16, 10, 20)

        val response = ParcelUnclaimedRankingResponse.from(
            listOf(
                arrivedParcel("박서연", "겨울 패딩", Zone.C, first),
                arrivedParcel("김민준", "문제집 세트", null, second),
            ),
        )

        assertEquals(listOf(1, 2), response.ranks.map { it.rank })
        assertEquals(listOf("박서연", "김민준"), response.ranks.map { it.ownerName })
        assertEquals(listOf(Zone.C, null), response.ranks.map { it.zone })
        assertEquals(listOf(first, second), response.ranks.map { it.arrivedAt })
    }

    @Test
    fun `도착 시각이 없는 택배는 빼고 순위를 이어서 매긴다`() {
        val response = ParcelUnclaimedRankingResponse.from(
            listOf(
                arrivedParcel("박서연", "겨울 패딩", Zone.C, null),
                arrivedParcel("김민준", "문제집 세트", Zone.A, LocalDateTime.of(2026, 11, 16, 10, 20)),
            ),
        )

        assertEquals(1, response.ranks.size)
        assertEquals(1, response.ranks.single().rank)
        assertEquals("김민준", response.ranks.single().ownerName)
    }

    @Test
    fun `방치된 택배가 없으면 빈 목록을 돌려준다`() {
        assertTrue(ParcelUnclaimedRankingResponse.from(emptyList()).ranks.isEmpty())
    }
}
