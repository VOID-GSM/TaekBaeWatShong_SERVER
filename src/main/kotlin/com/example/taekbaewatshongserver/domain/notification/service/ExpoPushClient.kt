package com.example.taekbaewatshongserver.domain.notification.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
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
    private val log = LoggerFactory.getLogger(javaClass)

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
     * `429`·`5xx`·네트워크 오류는 [RETRY_BACKOFFS] 간격으로 재시도하고, 그래도 실패하거나 다른 `4xx`면 호출자에게 그대로 던진다.
     */
    fun send(messages: List<ExpoPushMessage>): List<ExpoPushTicket> = messages
        .chunked(MAX_MESSAGES_PER_REQUEST)
        .flatMap { sendWithRetry(it) }

    private fun sendWithRetry(chunk: List<ExpoPushMessage>): List<ExpoPushTicket> {
        var attempt = 0
        while (true) {
            try {
                return sendChunk(chunk)
            } catch (e: RestClientException) {
                if (attempt >= RETRY_BACKOFFS.size || !isRetryable(e)) throw e

                val backoff = RETRY_BACKOFFS[attempt++]
                log.warn("[푸시 발송 재시도] {}/{}회 | {}ms 후 | 사유: {}", attempt, RETRY_BACKOFFS.size, backoff.toMillis(), describe(e))
                Thread.sleep(backoff.toMillis())
            }
        }
    }

    private fun sendChunk(chunk: List<ExpoPushMessage>): List<ExpoPushTicket> {
        val response = restClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(chunk)
            .retrieve()
            .body(ExpoPushResponse::class.java)

        return response?.data ?: throw ExpoPushException("Expo 응답에 티켓이 없습니다. errors=${response?.errors}")
    }

    /** 다시 보내도 결과가 같은 `4xx`(요청 형식 오류 등)는 재시도하지 않는다. */
    private fun isRetryable(e: RestClientException): Boolean = when (e) {
        is RestClientResponseException ->
            e.statusCode.is5xxServerError || e.statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()
        is ResourceAccessException -> true
        else -> false
    }

    private fun describe(e: RestClientException): String = when (e) {
        is RestClientResponseException -> "HTTP ${e.statusCode.value()}"
        else -> e.javaClass.simpleName
    }

    companion object {
        const val MAX_MESSAGES_PER_REQUEST = 100

        /** 재시도 간 대기 시간. 크기가 곧 최대 재시도 횟수다. */
        private val RETRY_BACKOFFS = listOf(Duration.ofSeconds(1), Duration.ofSeconds(3))
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
