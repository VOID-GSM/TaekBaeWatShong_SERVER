package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구역별 보관 택배 수")
data class ParcelZoneSummaryResponse(
    @field:Schema(description = "A~F 구역별 보관 택배 수. 택배가 없는 구역도 count 0으로 항상 포함된다.")
    val zones: List<ZoneCount>,
) {
    @Schema(description = "보관 구역 하나의 택배 수")
    data class ZoneCount(
        @field:Schema(description = "보관 구역", example = "A")
        val zone: Zone,
        @field:Schema(description = "이 구역에 보관 중인(ARRIVED) 택배 수", example = "3")
        val count: Int,
    )

    companion object {
        fun from(counts: Map<Zone, Long>): ParcelZoneSummaryResponse = ParcelZoneSummaryResponse(
            zones = Zone.entries.map { zone ->
                ZoneCount(zone = zone, count = (counts[zone] ?: 0L).toInt())
            },
        )
    }
}
