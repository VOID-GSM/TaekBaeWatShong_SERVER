package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.dto.request.DeviceTokenDeleteRequest
import com.example.taekbaewatshongserver.domain.notification.dto.request.DeviceTokenRegisterRequest
import com.example.taekbaewatshongserver.domain.notification.dto.response.DeviceTokenResponse
import com.example.taekbaewatshongserver.domain.notification.entity.DevicePlatform
import com.example.taekbaewatshongserver.domain.notification.entity.DeviceToken
import com.example.taekbaewatshongserver.domain.notification.repository.DeviceTokenRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.NotificationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    @Transactional
    fun registerDeviceToken(user: User, request: DeviceTokenRegisterRequest): DeviceTokenResponse {
        val platform = try {
            DevicePlatform.valueOf(request.platform)
        } catch (e: IllegalArgumentException) {
            throw NotificationException.InvalidPlatform("유효하지 않은 플랫폼입니다. (${request.platform})")
        }

        if (!ExpoPushClient.isExpoPushToken(request.deviceToken)) {
            throw NotificationException.InvalidDeviceToken()
        }

        val deviceToken = deviceTokenRepository.findByDeviceToken(request.deviceToken)
            ?.apply { reassign(user, platform) }
            ?: DeviceToken(
                deviceToken = request.deviceToken,
                platform = platform,
                owner = user,
            )

        return try {
            DeviceTokenResponse.from(deviceTokenRepository.save(deviceToken))
        } catch (e: DataIntegrityViolationException) {
            throw NotificationException.Conflict()
        }
    }

    @Transactional
    fun deleteDeviceToken(user: User, request: DeviceTokenDeleteRequest) {
        // 없는 토큰이나 이미 다른 사용자에게 넘어간 토큰은 지우지 않고 넘어간다. (로그아웃을 막지 않도록 멱등하게 처리)
        deviceTokenRepository.deleteByDeviceTokenAndOwner(request.deviceToken, user)
    }
}
