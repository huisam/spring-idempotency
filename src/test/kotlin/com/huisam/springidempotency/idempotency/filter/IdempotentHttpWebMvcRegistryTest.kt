package com.huisam.springidempotency.idempotency.filter

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.reflect.jvm.javaMethod

@ExtendWith(MockKExtension::class)
@Execution(ExecutionMode.CONCURRENT)
class IdempotentHttpWebMvcRegistryTest : WithAssertions {
    private lateinit var sut: IdempotentHttpWebMvcRegistry

    @MockK
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping

    private val requestMethod1 = RequestMethod.POST
    private val httpMethod1 = requestMethod1.name
    private val httpPath1 = "/api/v1/test"

    private val requestMethod2 = RequestMethod.GET
    private val httpMethod2 = requestMethod2.name
    private val httpPath2 = "/api/v1/test/{testId}"

    private val requestMappingInfo1 = RequestMappingInfo.paths(httpPath1).methods(requestMethod1).build()
    private val requestMappingInfo2 = RequestMappingInfo.paths(httpPath2).methods(requestMethod2).build()

    private val method1 = TestController::create.javaMethod!!
    private val method2 = TestController::find.javaMethod!!

    @BeforeEach
    fun setUp() {
        every { requestMappingHandlerMapping.handlerMethods } returns mapOf(
            requestMappingInfo1 to HandlerMethod(TestController(mockk()), method1),
            requestMappingInfo2 to HandlerMethod(TestController(mockk()), method2),
        )
        sut = IdempotentHttpWebMvcRegistry(requestMappingHandlerMapping)
    }

    @Test
    fun should_return_false_when_idempotency_http_operation_is_registered() {
        // when
        val actual = sut.notRegistered(httpMethod1, httpPath1)

        // then
        assertThat(actual).isFalse
    }

    @Test
    fun should_return_true_when_idempotency_http_operation_is_not_registered() {
        // when
        val actual = sut.notRegistered(httpMethod2, httpPath2.replace("{testId}", "1234"))

        // then
        assertThat(actual).isTrue
    }

    @Test
    fun should_return_true_when_handler_method_not_found_due_to_different_method() {
        // when
        val actual = sut.notRegistered("PUT", httpPath2.replace("{testId}", "1234"))

        // then
        assertThat(actual).isTrue
    }

    @Test
    fun should_return_true_when_handler_method_not_found_due_to_different_path() {
        // when
        val actual = sut.notRegistered(httpMethod1, "/not/exist/path")

        // then
        assertThat(actual).isTrue
    }
}