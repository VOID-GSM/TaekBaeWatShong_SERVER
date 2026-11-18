package com.example.taekbaewatshongserver.domain.notification.repository

import com.example.taekbaewatshongserver.domain.notification.entity.DeviceToken
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {

    fun findByDeviceToken(deviceToken: String): DeviceToken?

    fun deleteByDeviceTokenAndOwner(deviceToken: String, owner: User): Long
}
