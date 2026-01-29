package com.huisam.springidempotency.idempotency.filter

import com.huisam.springidempotency.idempotency.annotation.IdempotentHttpOperation
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Component
class IdempotentHttpWebMvcRegistry(
    requestMappingHandlerMapping: RequestMappingHandlerMapping,
) {
    private val handlerMethodsByRequestMapping: Map<RequestMappingInfo, HandlerMethod> =
        requestMappingHandlerMapping.handlerMethods

    fun notRegistered(httpMethod: String, httpPath: String): Boolean {
        val handlerMethod = handlerMethodsByRequestMapping.findHandlerMethodOrNull(
            requestMethod = RequestMethod.valueOf(httpMethod),
            pathContainer = PathContainer.parsePath(httpPath),
        )

        return handlerMethod == null ||
                !AnnotatedElementUtils.isAnnotated(handlerMethod.method, IdempotentHttpOperation::class.java)
    }

    private fun Map<RequestMappingInfo, HandlerMethod>.findHandlerMethodOrNull(
        requestMethod: RequestMethod,
        pathContainer: PathContainer,
    ): HandlerMethod? = this.entries.firstOrNull { (requestMapping, _) ->
        requestMapping.methodsCondition.methods.contains(requestMethod)
                && requestMapping.pathPatternsCondition!!.patterns.any { it.matches(pathContainer) }
    }?.value
}