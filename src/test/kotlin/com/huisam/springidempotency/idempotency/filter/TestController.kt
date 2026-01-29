package com.huisam.springidempotency.idempotency.filter

import com.huisam.springidempotency.idempotency.annotation.IdempotentHttpOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
internal class TestController(
    private val testService: TestService,
) {
    @IdempotentHttpOperation
    @PostMapping("/api/v1/test")
    fun create(
        @RequestBody requestBody: TestRequestBody
    ): ResponseEntity<TestResponseBody> {
        testService.noOperation()

        return ResponseEntity.ok(
            TestResponseBody(requestBody.request),
        )
    }

    @GetMapping("/api/v1/test/{testId}")
    fun find(
        @PathVariable testId: String,
    ): ResponseEntity<String> {
        return ResponseEntity.ok(testId)
    }

    data class TestRequestBody(
        val request: String?
    )

    data class TestResponseBody(
        val response: String?
    )
}

