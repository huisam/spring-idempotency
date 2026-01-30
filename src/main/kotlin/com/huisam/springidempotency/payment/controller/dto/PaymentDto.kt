package com.huisam.springidempotency.payment.controller.dto

import java.math.BigDecimal

data class PaymentDto(
    val paymentId: String,
    val amount: BigDecimal,
    val paymentMethod: String,
)