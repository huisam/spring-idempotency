package com.huisam.springidempotency.payment.controller

import com.huisam.springidempotency.idempotency.annotation.IdempotentHttpOperation
import com.huisam.springidempotency.payment.controller.dto.PaymentDto
import com.huisam.springidempotency.payment.controller.dto.PaymentPayRequestDto
import com.huisam.springidempotency.payment.controller.dto.PaymentPayResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/api/v1/payments")
@OptIn(ExperimentalUuidApi::class)
class PaymentController {

    @IdempotentHttpOperation
    @PostMapping("/pay")
    fun pay(
        @RequestBody paymentPayRequestDto: PaymentPayRequestDto
    ): ResponseEntity<PaymentPayResponseDto> {
        Thread.sleep(2000) // Simulate processing time

        return ResponseEntity.ok(
            PaymentPayResponseDto(
                paymentId = Uuid.random().toHexDashString()
            )
        )
    }

    @GetMapping("/{paymentId}")
    fun getPayment(
        @PathVariable paymentId: String,
    ): ResponseEntity<PaymentDto> {
        val paymentDto = PaymentDto(
            paymentId = paymentId,
            amount = 100.0.toBigDecimal(),
            paymentMethod = "Credit Card"
        )
        return ResponseEntity.ok(paymentDto)
    }
}