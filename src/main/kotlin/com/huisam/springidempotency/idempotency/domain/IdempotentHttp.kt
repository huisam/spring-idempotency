package com.huisam.springidempotency.idempotency.domain

data class IdempotentHttp(
    val idempotentHttpId: String,
    val request: IdempotentHttpRequest,
    val response: IdempotentHttpResponse?,
) {
    fun isDifferentRequest(request: IdempotentHttpRequest): Boolean {
        return this.request != request
    }

    fun recordResponse(response: IdempotentHttpResponse): IdempotentHttp {
        return copy(response = response)
    }
}

data class IdempotentHttpRequest(
    val method: String,
    val path: String,
    val contentType: String?,
    val body: String?
)

data class IdempotentHttpResponse(
    val status: Int,
    val contentType: String,
    val body: String
)
