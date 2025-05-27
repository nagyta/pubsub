package com.example.routing

import com.example.models.ConfigRequest
import com.example.service.interfaces.ICacheService
import com.example.service.interfaces.IRateLimitService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.routing.ConfigRoutes")

/**
 * Configures service configuration management endpoints.
 */
fun Application.configureConfigRoutes() {
    // Get services from Koin
    val cacheService = get<ICacheService>()
    val rateLimitService = get<IRateLimitService>()

    routing {
        // Service configuration management endpoints (Phase 4)
        route("/api/config") {
            // Get the current service configuration
            get {
                try {
                    val config = mapOf(
                        "cache" to cacheService.getConfiguration(),
                        "rateLimit" to rateLimitService.getConfiguration(),
                        "phase" to "Phase 4 - Management API"
                    )
                    call.respond(config)
                } catch (e: Exception) {
                    logger.error("Error getting service configuration: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error getting service configuration: ${e.message}")
                }
            }

            // Update service configuration
            put {
                try {
                    val configRequest = call.receive<ConfigRequest>()

                    // Update cache configuration
                    val cacheConfig = cacheService.updateConfiguration(
                        enabled = configRequest.cacheEnabled,
                        heapSize = configRequest.cacheHeapSize,
                        ttlMinutes = configRequest.cacheTtlSeconds / 60
                    )

                    // Update rate limit configuration
                    val rateLimitConfig = rateLimitService.updateConfiguration(
                        enabled = configRequest.rateLimitEnabled,
                        defaultLimit = configRequest.rateLimitPerMinute,
                        apiLimit = configRequest.rateLimitPerMinute / 2,
                        pubsubLimit = configRequest.rateLimitPerMinute * 2,
                        windowSize = 60
                    )

                    val updatedConfig = mapOf(
                        "cache" to cacheConfig,
                        "rateLimit" to rateLimitConfig,
                        "phase" to "Phase 4 - Management API"
                    )

                    call.respond(updatedConfig)
                } catch (e: Exception) {
                    logger.error("Error updating service configuration: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error updating service configuration: ${e.message}")
                }
            }
        }
    }
}
