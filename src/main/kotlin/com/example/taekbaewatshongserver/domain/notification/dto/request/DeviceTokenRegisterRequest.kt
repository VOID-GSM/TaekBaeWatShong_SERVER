package com.example.taekbaewatshongserver.domain.notification.dto.request

/**
 * @property deviceToken Expo 푸시 토큰. 앱에서 `getExpoPushTokenAsync()`로 받은 값을 그대로 보낸다.
 *   형식: `ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]`. 이 형식이 아닌 토큰은 저장은 되지만 푸시 발송 대상에서 빠진다.
 * @property platform `ANDROID`, `IOS`, `WEB` 중 하나
 */
data class DeviceTokenRegisterRequest(
    val deviceToken: String,
    val platform: String,
)
