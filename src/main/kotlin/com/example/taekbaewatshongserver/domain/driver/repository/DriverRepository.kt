package com.example.taekbaewatshongserver.domain.driver.repository

import com.example.taekbaewatshongserver.domain.driver.entity.Driver
import org.springframework.data.jpa.repository.JpaRepository

interface DriverRepository : JpaRepository<Driver, Long> {
    fun findByQrToken(qrToken: String): Driver?
}
