package com.huisam.springidempotency.idempotency.domain

class IdempotentHttpTestBuilder(
    var idempotentHttpId: String = "test-idempotency-http-id",
    var request: IdempotentHttpRequest = IdempotentHttpRequestTestBuilder().build(),
    var response: IdempotentHttpResponse? = IdempotentHttpResponseTestBuilder().build()
) {
    fun build() = IdempotentHttp(
        idempotentHttpId = idempotentHttpId,
        request = request,
        response = response,
    )
}
