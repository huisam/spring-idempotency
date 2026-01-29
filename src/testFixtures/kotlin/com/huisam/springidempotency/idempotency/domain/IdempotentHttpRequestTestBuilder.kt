package com.huisam.springidempotency.idempotency.domain

class IdempotentHttpRequestTestBuilder(
    var method: String = "POST",
    var path: String = "/test/path",
    var contentType: String? = null,
    var body: String? = null
) {
    fun build() = IdempotentHttpRequest(
        method = method,
        path = path,
        contentType = contentType,
        body = body,
    )
}
