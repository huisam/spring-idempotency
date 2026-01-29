package com.huisam.springidempotency.idempotency.annotation

import com.huisam.springidempotency.idempotency.domain.IdempotencyHeader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.core.annotation.AliasFor
import org.springframework.http.MediaType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@Operation(
    parameters = [
        Parameter(
            name = IdempotencyHeader.IDEMPOTENCY_KEY,
            `in` = ParameterIn.HEADER,
            schema = Schema(
                implementation = String::class,
                minLength = 1,
                maxLength = 100,
                examples = ["123e4567-e89b-12d3-a456-426614174000"],
            ),
            description = "`Idempotency-Key` for the request. If the same key is used, the response will be cached and returned without processing the request again.  \n" +
                    "This is useful for **retrying requests** without causing duplicate API operations. `Idempotency-Key` is maintained for **10 minutes**  \n" +
                    "The key should be **unique** for each API operation.(Strongly recommend **UUID v4**) ",
        ),
    ],
    responses = [
        ApiResponse(
            responseCode = "200",
            useReturnTypeSchema = true,
        ),
        ApiResponse(
            responseCode = "400",
            content = [
                Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = Schema(implementation = String::class),
                    examples = [ExampleObject(value = "Idempotency-Key must be 1 to 100 characters and not blank.")],
                ),
            ],
            description = "`Idempotency-Key` is invalid. It must be 1 to 100 characters and not blank.",
        ),
        ApiResponse(
            responseCode = "409",
            content = [
                Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = Schema(implementation = String::class),
                    examples = [ExampleObject(value = "Request is still processing. Please try again later.")],
                ),
            ],
            description = "Request is already processing with the same `Idempotency-Key`. You can retry with same `Idempotency-Key`.",
        ),
        ApiResponse(
            responseCode = "422",
            content = [
                Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = Schema(implementation = String::class),
                    examples = [ExampleObject(value = "Request does not equal with original request.")],
                ),
            ],
            description = "`Idempotency-Key` and request is different from original request. Please check your request and `Idempotency-Key`.",
        ),
    ],
)
annotation class IdempotentHttpOperation(
    @get:AliasFor(annotation = Operation::class, attribute = "summary")
    val summary: String = "",

    @get:AliasFor(annotation = Operation::class, attribute = "description")
    val description: String = "",
)
