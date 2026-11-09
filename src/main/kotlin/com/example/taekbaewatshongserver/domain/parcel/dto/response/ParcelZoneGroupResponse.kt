package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구역별 보관 택배")
data class ParcelZoneGroupResponse(
    @field:Schema(description = "구역별 묶음. 전체 조회 시 A~F 다음에 미배정(null) 그룹이 온다.")
    val zones: List<ZoneGroupDetail>,
) {
    @Schema(description = "보관 구역 하나의 택배 묶음")
    data class ZoneGroupDetail(
        @field:Schema(description = "보관 구역 (null이면 구역 미배정 그룹)", example = "A")
        val zone: Zone?,
        @field:Schema(description = "이 구역에 보관 중인 택배 수", example = "2")
        val count: Int,
        @field:Schema(description = "이 구역의 택배 목록 (등록 최신순)")
        val parcels: List<ParcelSimpleDetail>,
    )

    @Schema(description = "구역 안의 택배 요약")
    data class ParcelSimpleDetail(
        @field:Schema(description = "택배 ID", example = "1")
        val id: Long,
        @field:Schema(description = "운송장 번호", example = "123456789012")
        val invoiceNumber: String,
        @field:Schema(description = "택배 별칭", example = "운동화")
        val alias: String,
        @field:Schema(description = "택배 주인 이름", example = "홍길동")
        val ownerName: String,
        @field:Schema(description = "도착 처리 시각", example = "2026-11-09T14:30:00")
        val arrivedAt: java.time.LocalDateTime?,
        @field:Schema(description = "도착 후 찾아가지 않은 일수 (날짜 기준)", example = "2")
        val unclaimedDays: Int,
    )
}
