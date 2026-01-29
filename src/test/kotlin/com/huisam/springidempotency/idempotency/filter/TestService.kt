package com.huisam.springidempotency.idempotency.filter

import org.springframework.stereotype.Service

@Service
internal class TestService {
    fun noOperation() {
        // This service method is intentionally left empty to demonstrate idempotency.
        // In a real application, this could be a no-op or a placeholder for future logic.
    }
}
