package com.huisam.springidempotency.idempotency.filter

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * A class that allows reading the request body in [jakarta.servlet.Filter] and then reading the request body input stream again in dispatcherServlet
 */
internal class ContentCachingRequestWrapper(
    request: HttpServletRequest,
) : HttpServletRequestWrapper(request) {
    private val cachedBody: ByteArray = request.inputStream.use { it.readAllBytes() }

    /**
     * Returns the request body as a string. Returns null if the request body is empty.
     */
    val requestBody: String?
        get() = String(cachedBody).takeIf { it.isNotEmpty() }

    override fun getInputStream(): ServletInputStream {
        return CachedServletInputStream(cachedBody)
    }

    override fun getReader(): BufferedReader {
        return BufferedReader(InputStreamReader(inputStream))
    }

    private class CachedServletInputStream(cachedBody: ByteArray) : ServletInputStream() {
        private val inputStream: ByteArrayInputStream = ByteArrayInputStream(cachedBody)

        override fun read(): Int {
            return inputStream.read()
        }

        override fun isFinished(): Boolean {
            return inputStream.available() == 0
        }

        override fun isReady(): Boolean {
            return true
        }

        override fun setReadListener(readListener: ReadListener) {
            // Not implemented for this request caching
        }
    }
}
