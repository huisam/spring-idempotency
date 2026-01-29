package com.huisam.springidempotency.idempotency.repository

import com.huisam.springidempotency.idempotency.domain.IdempotentHttp
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration

@Component
class IdempotentHttpRepository(
    private val objectMapper: ObjectMapper,
    private val stringRedisTemplate: StringRedisTemplate,
) {
    private val ops = stringRedisTemplate.opsForValue()

    fun save(idempotentHttp: IdempotentHttp) {
        val jsonString = objectMapper.writeValueAsString(idempotentHttp)
        ops.set(idempotentHttp.idempotentHttpId, jsonString, TIME_TO_LIVE)
    }

    fun deleteById(idempotentHttpId: String) {
        stringRedisTemplate.delete(idempotentHttpId)
    }

    fun findByIdOrNull(idempotentHttpId: String): IdempotentHttp? {
        return ops.get(idempotentHttpId)
            ?.let { objectMapper.readValue(it) }
    }

    companion object {
        private val TIME_TO_LIVE = Duration.ofMinutes(10)
    }
}