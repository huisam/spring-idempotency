package com.huisam.springidempotency.idempotency.filter

import com.huisam.springidempotency.idempotency.domain.IdempotencyHeader
import com.huisam.springidempotency.idempotency.domain.IdempotentHttp
import com.huisam.springidempotency.idempotency.domain.IdempotentHttpRequest
import com.huisam.springidempotency.idempotency.domain.IdempotentHttpResponse
import com.huisam.springidempotency.idempotency.repository.IdempotentHttpRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.servlet.filter.OrderedFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

/**
 * Filter for managing idempotent HTTP requests/responses based on [IdempotencyHeader.IDEMPOTENCY_KEY] value
 *
 * Since IdempotentHttpId is generated based on HTTP method and path, clients need to request a unique [IdempotencyHeader.IDEMPOTENCY_KEY] for each API.
 *
 * @property order The filter execution order defaults to the order before entering [org.springframework.web.servlet.DispatcherServlet]
 */
@Component
class IdempotentHttpWebMvcFilter(
    private val idempotentHttpWebMvcRegistry: IdempotentHttpWebMvcRegistry,
    private val idempotentHttpRepository: IdempotentHttpRepository,
    private val order: Int = Ordered.LOWEST_PRECEDENCE - 1,
) : OncePerRequestFilter(), OrderedFilter {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val httpMethod = request.method
        val httpPath = request.requestURI
        val idempotencyKey = request.getHeader(IdempotencyHeader.IDEMPOTENCY_KEY)

        when {
            idempotencyKey == null -> filterChain.doFilter(request, response)

            idempotencyKey.isBlank() || idempotencyKey.length > 100 -> {
                response.status = HttpStatus.BAD_REQUEST.value()
                response.contentType = MediaType.TEXT_PLAIN_VALUE
                response.writer.write("Idempotency-Key must be 1 to 100 characters and not blank.")
            }

            idempotentHttpWebMvcRegistry.notRegistered(httpMethod, httpPath) -> {
                response.status = HttpStatus.BAD_REQUEST.value()
                response.contentType = MediaType.TEXT_PLAIN_VALUE
                response.writer.write("Idempotency-Key is not allowed on $httpMethod $httpPath")
            }

            else -> processIdempotentHttp(
                idempotentHttpId = createIdempotentHttpId(httpMethod, httpPath, idempotencyKey),
                request = request,
                response = response,
                filterChain = filterChain,
            )
        }
    }

    private fun createIdempotentHttpId(httpMethod: String, httpPath: String, idempotencyKey: String): String {
        return "$httpMethod::$httpPath::$idempotencyKey"
    }

    private fun processIdempotentHttp(
        idempotentHttpId: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cachingRequest = ContentCachingRequestWrapper(request)
        val idempotentHttp = IdempotentHttp(
            idempotentHttpId = idempotentHttpId,
            request = IdempotentHttpRequest(
                method = cachingRequest.method,
                path = cachingRequest.requestURI,
                contentType = cachingRequest.contentType,
                body = cachingRequest.requestBody,
            ),
            response = null,
        )
        val existedIdempotentHttp = idempotentHttpRepository.findByIdOrNull(idempotentHttp.idempotentHttpId)

        when {
            existedIdempotentHttp == null -> {
                doIdempotentHttpFilter(idempotentHttp, cachingRequest, response, filterChain)
            }

            existedIdempotentHttp.isDifferentRequest(idempotentHttp.request) -> {
                response.status = HttpStatus.UNPROCESSABLE_ENTITY.value()
                response.contentType = MediaType.TEXT_PLAIN_VALUE
                response.writer.write("Request does not equal with original request.")
            }

            existedIdempotentHttp.response == null -> {
                response.status = HttpStatus.CONFLICT.value()
                response.contentType = MediaType.TEXT_PLAIN_VALUE
                response.writer.write("Request is still processing. Please try again later.")
                log.warn("Idempotent request is still processing. Please check the server latency or client retry interval --> $idempotentHttpId")
            }

            else -> {
                response.status = existedIdempotentHttp.response.status
                response.contentType = existedIdempotentHttp.response.contentType
                response.writer.write(existedIdempotentHttp.response.body)
            }
        }
    }

    private fun doIdempotentHttpFilter(
        idempotentHttp: IdempotentHttp,
        cachingRequest: ContentCachingRequestWrapper,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        recordRequest(idempotentHttp)

        val cachingResponse = ContentCachingResponseWrapper(response)
        try {
            filterChain.doFilter(cachingRequest, cachingResponse)

            recordResponse(idempotentHttp, cachingResponse)
            log.info("Success idempotent request --> ${idempotentHttp.idempotentHttpId}")
        } finally {
            // Respond cached response to client buffer
            cachingResponse.copyBodyToResponse()
        }
    }

    private fun recordRequest(idempotentHttp: IdempotentHttp) {
        idempotentHttpRepository.save(idempotentHttp)
    }

    /**
     * If saving the response fails, the request was processed but the response was not saved.
     * In this case, since reliable idempotent HTTP cannot be guaranteed, delete the HTTP record itself to avoid responding with a conflict(409) status.
     */
    private fun recordResponse(
        idempotentHttp: IdempotentHttp,
        cachingResponse: ContentCachingResponseWrapper
    ) {
        runCatching {
            val idempotentHttpResponse = IdempotentHttpResponse(
                status = cachingResponse.status,
                contentType = cachingResponse.contentType,
                body = String(cachingResponse.contentAsByteArray),
            )
            idempotentHttpRepository.save(idempotentHttp.recordResponse(idempotentHttpResponse))
        }.onFailure {
            log.error("Error while recording response for idempotencyHttp: ${idempotentHttp.idempotentHttpId}", it)
            deleteIdempotentHttp(idempotentHttp.idempotentHttpId)
        }
    }

    private fun deleteIdempotentHttp(idempotentHttpId: String) {
        runCatching {
            idempotentHttpRepository.deleteById(idempotentHttpId)
        }.onFailure {
            log.error("Error while deleting idempotentHttp: $idempotentHttpId", it)
        }
    }

    override fun getOrder(): Int = order
}
