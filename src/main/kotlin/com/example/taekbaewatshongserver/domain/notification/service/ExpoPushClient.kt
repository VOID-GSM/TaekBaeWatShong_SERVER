package com.example.taekbaewatshongserver.domain.notification.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * Expo Push API 클라이언트.
 * https://docs.expo.dev/push-notifications/sending-notifications/
 */
@Component
class ExpoPushClient(
    @Value("\${notification.push.expo.url:https://exp.host/--/api/v2/push/send}")
    url: String,
    @Value("\${notification.push.expo.access-token:}")
    accessToken: String,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(url)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(5))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .apply { if (accessToken.isNotBlank()) it.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $accessToken") }
        .build()

    /**
     * 메시지를 최대 [MAX_MESSAGES_PER_REQUEST]개씩 나눠 보내고, 요청 순서와 같은 순서의 티켓 목록을 돌려준다.
     * 네트워크/HTTP 오류는 호출자에게 그대로 던진다.
     */
    fun send(messages: List<ExpoPushMessage>): List<ExpoPushTicket> = messages
        .chunked(MAX_MESSAGES_PER_REQUEST)
        .flatMap { chunk ->
            val response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(chunk)
                .retrieve()
                .body(ExpoPushResponse::class.java)

            response?.data ?: throw ExpoPushException("Expo 응답에 티켓이 없습니다. errors=${response?.errors}")
        }

    companion object {
        const val MAX_MESSAGES_PER_REQUEST = 100
        private val TOKEN_PATTERN = Regex("^Expo(nent)?PushToken\\[.+]$")

        fun isExpoPushToken(token: String): Boolean = TOKEN_PATTERN.matches(token)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpoPushMessage(
    val to: String,
    val title: String,
    val body: String,
    val data: Map<String, Any>,
    val sound: String? = "default",
    val priority: String? = "high",
    val channelId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExpoPushResponse(
    val data: List<ExpoPushTicket>? = null,
    val errors: List<Map<String, Any?>>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExpoPushTicket(
    val status: String,
    val id: String? = null,
    val message: String? = null,
    val details: Details? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Details(
        val error: String? = null,
    )

    val isOk: Boolean get() = status == "ok"
    val isDeviceNotRegistered: Boolean get() = details?.error == "DeviceNotRegistered"
}

class ExpoPushException(message: String) : RuntimeException(message)
