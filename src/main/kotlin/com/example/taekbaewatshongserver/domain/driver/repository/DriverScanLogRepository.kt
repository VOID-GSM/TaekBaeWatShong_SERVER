package com.example.taekbaewatshongserver.domain.driver.repository

import com.example.taekbaewatshongserver.domain.driver.entity.DriverScanLog
import org.springframework.data.jpa.repository.JpaRepository

interface DriverScanLogRepository : JpaRepository<DriverScanLog, Long>
