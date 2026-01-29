package com.huisam.springidempotency.idempotency.domain

class IdempotentHttpResponseTestBuilder(
    var status: Int = 200,
    var contentType: String = "application/json",
    var body: String = "{}"
) {
    fun build() = IdempotentHttpResponse(
        status = status,
        contentType = contentType,
        body = body,
    )
}
