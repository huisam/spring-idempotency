package com.huisam.springidempotency.idempotency.domain

import com.huisam.springidempotency.WithIdempotencyTestFixtures
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class IdempotentHttpTest : WithAssertions, WithIdempotencyTestFixtures {

    @Test
    fun should_return_true_when_request_info_is_different() {
        // given
        val idempotentHttp1 = idempotentHttp {
            idempotentHttpId = "idempotentHttpId1"
            request = idempotentHttpRequest {
                method = "POST"
                path = "/api/v1/test"
                contentType = "application/json"
                body = """{"key":"value"}"""
            }
        }
        val idempotentHttpRequest1 = idempotentHttpRequest {
            method = "POST"
            path = "/api/v1/test"
            contentType = "application/json"
            body = """{"key":"differentValue"}"""
        }

        // when
        val actual = idempotentHttp1.isDifferentRequest(idempotentHttpRequest1)

        // then
        assertThat(actual).isTrue
    }

    @Test
    fun should_return_false_when_request_info_is_same() {
        // given
        val idempotentHttp1 = idempotentHttp {
            idempotentHttpId = "idempotentHttpId1"
            request = idempotentHttpRequest {
                method = "POST"
                path = "/api/v1/test"
                contentType = "application/json"
                body = """{"key":"value"}"""
            }
        }

        // when
        val actual = idempotentHttp1.isDifferentRequest(idempotentHttp1.request)

        // then
        assertThat(actual).isFalse
    }

    @Test
    fun should_record_response_and_create_object_with_response_info() {
        // given
        val idempotentHttp1 = idempotentHttp {
            idempotentHttpId = "idempotentHttpId1"
            response = null
        }
        val idempotentHttpResponse = idempotentHttpResponse {
            status = 200
            contentType = "application/json"
            body = """{"key":"value"}"""
        }

        // when
        val actual = idempotentHttp1.recordResponse(idempotentHttpResponse)

        // then
        assertThat(actual).isEqualTo(
            idempotentHttp {
                idempotentHttpId = idempotentHttp1.idempotentHttpId
                request = idempotentHttp1.request
                response = idempotentHttpResponse
            },
        )
    }
}
