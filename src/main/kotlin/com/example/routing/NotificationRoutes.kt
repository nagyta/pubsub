package com.example.routing

import com.example.service.interfaces.INotificationConsumerService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.routing.NotificationRoutes")

/**
 * Configures notification consumer management endpoints.
 */
fun Application.configureNotificationRoutes() {
    // Get services from Koin
    val notificationConsumerService = get<INotificationConsumerService>()

    routing {
        // Notification consumer management endpoints (Phase 3)
        route("/api/notifications") {
            // Get consumer status
            get("/consumer/status") {
                try {
                    val status = mapOf(
                        "running" to notificationConsumerService.isRunning(),
                        "phase" to "Phase 3 - Notification Consumer"
                    )
                    call.respond(status)
                } catch (e: Exception) {
                    logger.error("Error getting consumer status: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error getting consumer status: ${e.message}")
                }
            }

            // Start consumer
            post("/consumer/start") {
                try {
                    notificationConsumerService.startConsuming()
                    call.respond(HttpStatusCode.OK, "Notification consumer started")
                } catch (e: Exception) {
                    logger.error("Error starting consumer: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error starting consumer: ${e.message}")
                }
            }

            // Stop consumer
            post("/consumer/stop") {
                try {
                    notificationConsumerService.stopConsuming()
                    call.respond(HttpStatusCode.OK, "Notification consumer stopped")
                } catch (e: Exception) {
                    logger.error("Error stopping consumer: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error stopping consumer: ${e.message}")
                }
            }
        }
    }
}
