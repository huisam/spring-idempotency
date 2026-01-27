package com.huisam.springidempotency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringIdempotencyApplication

fun main(args: Array<String>) {
    runApplication<SpringIdempotencyApplication>(*args)
}
