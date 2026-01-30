package com.huisam.springidempotency.payment.controller.dto

import java.math.BigDecimal

data class PaymentPayRequestDto(
    val amount: BigDecimal,
    val paymentMethod: String
)
