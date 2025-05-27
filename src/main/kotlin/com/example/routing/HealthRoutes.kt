package com.example.routing

import com.example.models.SubscriptionEntity
import com.example.service.interfaces.ICacheService
import com.example.service.interfaces.INotificationQueueService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.routing.HealthRoutes")

/**
 * Configures health check endpoints for load balancing and Kubernetes.
 */
fun Application.configureHealthRoutes() {
    // Get services from Koin
    val cacheService = get<ICacheService>()
    val notificationQueueService = get<INotificationQueueService>()

    routing {
        // Health check endpoints for load balancing and Kubernetes
        route("/health") {
            // Basic health check
            get {
                call.respond(mapOf(
                    "status" to "UP",
                    "instance" to (System.getenv("INSTANCE_ID") ?: "unknown"),
                    "timestamp" to System.currentTimeMillis()
                ))
            }

            // Readiness check
            get("/ready") {
                // Check if all required services are available
                val allServicesAvailable = try {
                    // Check database connection
                    val dbAvailable = transaction { 
                        try {
                            SubscriptionEntity.all().limit(1).count() >= 0
                        } catch (e: Exception) {
                            logger.error("Database health check failed: ${e.message}", e)
                            false
                        }
                    }

                    // Check RabbitMQ connection
                    val rabbitMQAvailable = notificationQueueService.isAvailable()

                    // Check cache
                    val cacheAvailable = cacheService.isAvailable()

                    // All services must be available
                    dbAvailable && rabbitMQAvailable && cacheAvailable
                } catch (e: Exception) {
                    logger.error("Health check failed: ${e.message}", e)
                    false
                }

                val instanceId = System.getenv("INSTANCE_ID") ?: "unknown"
                if (allServicesAvailable) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status" to "READY",
                        "instance" to instanceId,
                        "timestamp" to System.currentTimeMillis()
                    ))
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                        "status" to "NOT_READY",
                        "instance" to instanceId,
                        "timestamp" to System.currentTimeMillis()
                    ))
                }
            }
        }
    }
}
