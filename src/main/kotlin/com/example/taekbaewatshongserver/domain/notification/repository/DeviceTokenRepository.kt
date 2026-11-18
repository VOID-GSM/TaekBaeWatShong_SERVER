package com.example.taekbaewatshongserver.domain.notification.repository

import com.example.taekbaewatshongserver.domain.notification.entity.DeviceToken
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {

    fun findByDeviceToken(deviceToken: String): DeviceToken?

    fun findAllByOwnerId(ownerId: Long): List<DeviceToken>

    fun deleteByDeviceTokenAndOwner(deviceToken: String, owner: User): Long

    @Transactional
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.deviceToken IN :deviceTokens")
    fun deleteAllByDeviceTokenIn(@Param("deviceTokens") deviceTokens: Collection<String>): Int
}
