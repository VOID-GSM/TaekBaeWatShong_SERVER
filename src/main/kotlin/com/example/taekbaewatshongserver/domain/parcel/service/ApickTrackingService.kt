package com.example.taekbaewatshongserver.domain.parcel.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import com.example.taekbaewatshongserver.domain.parcel.dto.response.ApickTrackingResponse
import java.time.Duration

@Service
class ApickTrackingService(
    @Value("\${apick.api-key}")
    private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://apick.app/rest")
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(3))
        })
        .build()

    fun validateInvoice(deliveryCompany: String, invoiceNumber: String): Boolean {
        if (apiKey.isBlank() || apiKey == "dummy-key") {
            log.warn("APICK API 키가 설정되지 않아 운송장 검증을 패스합니다.")
            return true
        }

        val companyCode = convertCompanyToCode(deliveryCompany)

        return try {
            val response = restClient.get()
                .uri("/parcel_tracking?invoice_number={invoiceNumber}&delivery_company={companyCode}", invoiceNumber, companyCode)
                .header("CLOB-API-KEY", apiKey)
                .retrieve()
                .body(ApickTrackingResponse::class.java)

            response?.success == true && response.data != null

        } catch (e: RestClientResponseException) {
            log.error("APICK API 응답 에러 발생 (HTTP ${e.statusCode}): ${e.responseBodyAsString}", e)

            if (e.statusCode.is5xxServerError) {
                log.warn("APICK 서버 장애로 인해 운송장 검증을 임시 통과(Bypass)합니다. [운송장: $invoiceNumber]")
                return true
            }
            false

        } catch (e: Exception) {
            log.error("APICK API 연동 중 네트워크/시스템 오류 발생: ${e.message}", e)
            log.warn("외부 API 연동 실패로 인해 운송장 검증을 임시 통과(Bypass)합니다. [운송장: $invoiceNumber]")
            true
        }
    }

    private fun convertCompanyToCode(companyName: String): String {
        return when (companyName.replace(" ", "")) {
            "CJ대한통운", "CJ" -> "cjlogistics"
            "우체국택배", "우체국" -> "epost"
            "한진택배", "한진" -> "hanjin"
            "롯데택배", "롯데" -> "lotte"
            "로젠택배", "로젠" -> "logen"
            "GS25편의점", "GS25" -> "cvsnet"
            "CU편의점", "CU" -> "cupost"
            else -> companyName
        }
    }
}