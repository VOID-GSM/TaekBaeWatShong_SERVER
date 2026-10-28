package com.example.taekbaewatshongserver.domain.driver.service

import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrCreateRequest
import com.example.taekbaewatshongserver.domain.driver.dto.request.DriverQrScanRequest
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrCreateResponse
import com.example.taekbaewatshongserver.domain.driver.dto.response.DriverQrScanResponse
import com.example.taekbaewatshongserver.domain.driver.entity.Driver
import com.example.taekbaewatshongserver.domain.driver.entity.DriverScanLog
import com.example.taekbaewatshongserver.domain.driver.repository.DriverRepository
import com.example.taekbaewatshongserver.domain.driver.repository.DriverScanLogRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.DriverException
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DriverService(
    private val driverRepository: DriverRepository,
    private val driverScanLogRepository: DriverScanLogRepository,
) {

    @Transactional
    fun createQr(request: DriverQrCreateRequest): DriverQrCreateResponse {
        val driver = driverRepository.save(
            Driver(
                driverName = request.driverName,
                deliveryCompany = request.deliveryCompany,
                qrToken = UUID.randomUUID().toString(),
            ),
        )

        return DriverQrCreateResponse.from(driver, generateQrImageDataUrl(driver.qrToken))
    }

    private fun generateQrImageDataUrl(qrToken: String): String {
        val matrix = QRCodeWriter().encode(qrToken, BarcodeFormat.QR_CODE, QR_IMAGE_SIZE, QR_IMAGE_SIZE)
        val png = ByteArrayOutputStream().use {
            MatrixToImageWriter.writeToStream(matrix, "PNG", it)
            it.toByteArray()
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png)
    }

    @Transactional
    fun scan(user: User, request: DriverQrScanRequest): DriverQrScanResponse {
        val driver = driverRepository.findByQrToken(request.qrToken)
            ?: throw DriverException.NotFound()

        val log = driverScanLogRepository.save(DriverScanLog(driver = driver, scannedBy = user))
        return DriverQrScanResponse.from(log)
    }

    companion object {
        private const val QR_IMAGE_SIZE = 300
    }
}
