package com.huisam.springidempotency.idempotency.filter

import com.huisam.springidempotency.WithIdempotencyTestFixtures
import com.huisam.springidempotency.idempotency.domain.IdempotencyHeader
import com.huisam.springidempotency.idempotency.repository.IdempotentHttpRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import java.util.*

@ExtendWith(MockKExtension::class)
@Execution(ExecutionMode.CONCURRENT)
class IdempotentHttpWebMvcFilterTest : WithIdempotencyTestFixtures, WithAssertions {

    private lateinit var sut: IdempotentHttpWebMvcFilter

    @MockK
    private lateinit var idempotentHttpWebMvcRegistry: IdempotentHttpWebMvcRegistry

    @MockK
    private lateinit var idempotentHttpRepository: IdempotentHttpRepository

    @MockK(relaxed = true)
    private lateinit var testService: TestService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        sut = IdempotentHttpWebMvcFilter(idempotentHttpWebMvcRegistry, idempotentHttpRepository)
        mockMvc = MockMvcBuilders.standaloneSetup(TestController(testService))
            .addFilters<StandaloneMockMvcBuilder>(sut)
            .build()
    }

    private val idempotencyKey1 = UUID.randomUUID().toString()

    private val requestBody1 = """{"request": "success"}"""
    private val requestBody2 = """{"request": "different"}"""
    private val responseBody1 = """{"response":"success"}"""

    @Test
    fun should_not_process_idempotent_http_when_idempotency_key_is_missing() {
        // when & then
        mockMvc.post("/api/v1/test") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isOk() }
            content { json(responseBody1) }
        }

        verify {
            idempotentHttpWebMvcRegistry wasNot called
            idempotentHttpRepository wasNot called
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun should_return_400_when_idempotency_key_is_blank_or_empty(idempotencyKey: String) {
        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isBadRequest() }
            content { string("Idempotency-Key must be 1 to 100 characters and not blank.") }
        }
    }

    @Test
    fun should_return_400_when_idempotency_key_exceeds_100_characters() {
        // given
        val longIdempotencyKey = "a".repeat(101)

        // when
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, longIdempotencyKey)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isBadRequest() }
            content { string("Idempotency-Key must be 1 to 100 characters and not blank.") }
        }
    }

    @Test
    fun should_return_400_when_not_registered_in_registry() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns true

        // when & then
        mockMvc.get("/api/v1/test/1234") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
        }.andExpect {
            status { isBadRequest() }
            content { string("Idempotency-Key is not allowed on GET /api/v1/test/1234") }
        }

        verify(exactly = 1) {
            idempotentHttpWebMvcRegistry.notRegistered("GET", "/api/v1/test/1234")
            idempotentHttpRepository wasNot called
        }
        confirmVerified(idempotentHttpWebMvcRegistry, idempotentHttpRepository)
    }

    @Test
    fun should_call_service_and_record_response_when_idempotent_http_not_saved() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns null
        every { idempotentHttpRepository.save(any()) } just runs

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isOk() }
            content { json(responseBody1) }
        }

        verifySequence {
            idempotentHttpWebMvcRegistry.notRegistered("POST", "/api/v1/test")
            idempotentHttpRepository.findByIdOrNull("POST::/api/v1/test::$idempotencyKey1")
            idempotentHttpRepository.save(
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = null
                },
            )
            testService.noOperation()
            idempotentHttpRepository.save(
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = idempotentHttpResponse {
                        status = 200
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = responseBody1
                    }
                },
            )
        }
        confirmVerified(idempotentHttpWebMvcRegistry, idempotentHttpRepository, testService)
    }

    @Test
    fun should_delete_idempotent_http_when_exception_occurs_while_saving_response() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns null
        every { idempotentHttpRepository.save(any()) } just runs andThenThrows RuntimeException(
            "Database error",
        )
        every { idempotentHttpRepository.deleteById(any()) } just runs

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isOk() }
            content { json(responseBody1) }
        }

        verifySequence {
            idempotentHttpWebMvcRegistry.notRegistered("POST", "/api/v1/test")
            idempotentHttpRepository.findByIdOrNull("POST::/api/v1/test::$idempotencyKey1")
            idempotentHttpRepository.save(
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = null
                },
            )
            testService.noOperation()
            idempotentHttpRepository.save(
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = idempotentHttpResponse {
                        status = 200
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = responseBody1
                    }
                },
            )
            idempotentHttpRepository.deleteById("POST::/api/v1/test::$idempotencyKey1")
        }
        confirmVerified(idempotentHttpWebMvcRegistry, idempotentHttpRepository, testService)
    }

    @Test
    fun should_not_throw_exception_when_delete_fails_after_response_save_error() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns null
        every { idempotentHttpRepository.save(any()) } just runs andThenThrows RuntimeException("Database error")
        every { idempotentHttpRepository.deleteById(any()) } throws RuntimeException("Database error")

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isOk() }
            content { json(responseBody1) }
        }
    }

    @Test
    fun should_return_422_when_different_request_body_with_same_idempotency_key() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = null
                }

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody2
        }.andExpect {
            status { isUnprocessableContent() }
            content { string("Request does not equal with original request.") }
        }
        verify {
            testService wasNot called
        }
    }

    @Test
    fun should_return_409_when_same_idempotency_request_is_already_processing() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = null
                }

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isConflict() }
            content { string("Request is still processing. Please try again later.") }
        }
        verify {
            testService wasNot called
        }
    }

    @Test
    fun should_return_saved_response_when_idempotency_key_already_exists() {
        // given
        every { idempotentHttpWebMvcRegistry.notRegistered(any(), any()) } returns false
        every { idempotentHttpRepository.findByIdOrNull(any()) } returns
                idempotentHttp {
                    idempotentHttpId = "POST::/api/v1/test::$idempotencyKey1"
                    request = idempotentHttpRequest {
                        method = "POST"
                        path = "/api/v1/test"
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = requestBody1
                    }
                    response = idempotentHttpResponse {
                        status = 200
                        contentType = MediaType.APPLICATION_JSON_VALUE
                        body = responseBody1
                    }
                }

        // when & then
        mockMvc.post("/api/v1/test") {
            header(IdempotencyHeader.IDEMPOTENCY_KEY, idempotencyKey1)
            contentType = MediaType.APPLICATION_JSON
            content = requestBody1
        }.andExpect {
            status { isOk() }
            content { json(responseBody1) }
        }

        verify(exactly = 1) {
            idempotentHttpWebMvcRegistry.notRegistered("POST", "/api/v1/test")
            idempotentHttpRepository.findByIdOrNull("POST::/api/v1/test::$idempotencyKey1")
        }
        verify(exactly = 0) {
            idempotentHttpRepository.save(any())
            testService.noOperation()
        }
        confirmVerified(idempotentHttpWebMvcRegistry, idempotentHttpRepository, testService)
    }

    @Test
    fun order() {
        // when
        val actual = sut.order

        // then
        assertThat(actual).isEqualTo(Int.MAX_VALUE - 1)
    }
}
