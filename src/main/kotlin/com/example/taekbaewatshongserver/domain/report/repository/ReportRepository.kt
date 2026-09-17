package com.example.taekbaewatshongserver.domain.report.repository

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReportRepository : JpaRepository<Report, Long> {

    fun existsByParcel(parcel: Parcel): Boolean

    @EntityGraph(attributePaths = ["parcel"])
    fun findAllByReporterOrderByCreatedAtDesc(reporter: User): List<Report>

    @EntityGraph(attributePaths = ["parcel", "reporter"])
    fun findAllByOrderByCreatedAtDesc(): List<Report>

    @EntityGraph(attributePaths = ["parcel", "reporter"])
    fun findAllByProgressOrderByCreatedAtDesc(progress: ReportProgress): List<Report>
}
