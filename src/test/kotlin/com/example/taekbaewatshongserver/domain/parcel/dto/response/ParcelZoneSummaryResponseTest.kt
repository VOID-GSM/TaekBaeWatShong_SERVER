package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ParcelZoneSummaryResponseTest {

    @Test
    fun `택배가 없는 구역도 0으로 포함해 A부터 F까지 순서대로 돌려준다`() {
        val response = ParcelZoneSummaryResponse.from(mapOf(Zone.C to 5L, Zone.A to 3L))

        assertEquals(Zone.entries.toList(), response.zones.map { it.zone })
        assertEquals(listOf(3, 0, 5, 0, 0, 0), response.zones.map { it.count })
    }

    @Test
    fun `보관 중인 택배가 하나도 없으면 모든 구역이 0이다`() {
        val response = ParcelZoneSummaryResponse.from(emptyMap())

        assertEquals(6, response.zones.size)
        assertEquals(List(6) { 0 }, response.zones.map { it.count })
    }
}
