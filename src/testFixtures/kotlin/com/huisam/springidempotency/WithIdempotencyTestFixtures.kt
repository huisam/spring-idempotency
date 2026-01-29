package com.huisam.springidempotency

import com.huisam.springidempotency.idempotency.domain.IdempotentHttpRequestTestBuilder
import com.huisam.springidempotency.idempotency.domain.IdempotentHttpResponseTestBuilder
import com.huisam.springidempotency.idempotency.domain.IdempotentHttpTestBuilder

interface WithIdempotencyTestFixtures {
    fun idempotentHttp(initialize: IdempotentHttpTestBuilder.() -> Unit) =
        IdempotentHttpTestBuilder().apply(initialize).build()

    fun idempotentHttpRequest(initialize: IdempotentHttpRequestTestBuilder.() -> Unit) =
        IdempotentHttpRequestTestBuilder().apply(initialize).build()

    fun idempotentHttpResponse(initialize: IdempotentHttpResponseTestBuilder.() -> Unit) =
        IdempotentHttpResponseTestBuilder().apply(initialize).build()
}