package com.example.routing

import com.example.models.StatusRequest
import com.example.models.SubscriptionRequest
import com.example.repository.interfaces.ISubscriptionRepository
import com.example.service.interfaces.IPubSubHubbubService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.routing.SubscriptionRoutes")

/**
 * Configures subscription management endpoints.
 */
fun Application.configureSubscriptionRoutes() {
    // Get services from Koin
    val subscriptionRepository = get<ISubscriptionRepository>()
    val pubSubHubbubService = get<IPubSubHubbubService>()

    routing {
        // Subscription management endpoints (Phase 4 - Management API)
        route("/api/subscriptions") {
            // Get all active subscriptions
            get {
                try {
                    val subscriptions = subscriptionRepository.getAllActiveSubscriptions()
                    call.respond(subscriptions)
                } catch (e: Exception) {
                    logger.error("Error retrieving active subscriptions: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving active subscriptions: ${e.message}")
                }
            }

            // Get all subscriptions (including inactive)
            get("/all") {
                try {
                    val subscriptions = subscriptionRepository.getAllSubscriptions()
                    call.respond(subscriptions)
                } catch (e: Exception) {
                    logger.error("Error retrieving all subscriptions: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving all subscriptions: ${e.message}")
                }
            }

            // Get a specific subscription by channel ID
            get("/{channelId}") {
                try {
                    val channelId = call.parameters["channelId"] ?: throw IllegalArgumentException("Missing channelId parameter")
                    val subscription = subscriptionRepository.getSubscription(channelId)

                    if (subscription != null) {
                        call.respond(subscription)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Subscription not found for channel ID: $channelId")
                    }
                } catch (e: Exception) {
                    logger.error("Error retrieving subscription: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving subscription: ${e.message}")
                }
            }

            // Create a new subscription
            post {
                // This is a suspend function because it calls the pubSubHubbubService.sendSubscriptionRequest suspend function
                try {
                    val subscriptionRequest = call.receive<SubscriptionRequest>()

                    // Validate request
                    if (subscriptionRequest.channelId.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Channel ID is required")
                        return@post
                    }

                    if (subscriptionRequest.topic.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Topic URL is required")
                        return@post
                    }

                    if (subscriptionRequest.callbackUrl.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Callback URL is required")
                        return@post
                    }

                    if (subscriptionRequest.leaseSeconds <= 0) {
                        call.respond(HttpStatusCode.BadRequest, "Lease seconds must be greater than 0")
                        return@post
                    }

                    // Always use our fixed callback URL regardless of what was provided in the request
                    val callbackUrl = System.getenv("CALLBACK_URL") ?: throw IllegalStateException("Missing CALLBACK_URL environment variable")

                    // Create or update the subscription in the database
                    val subscription = subscriptionRepository.createOrUpdateSubscription(
                        channelId = subscriptionRequest.channelId,
                        topic = subscriptionRequest.topic,
                        callbackUrl = callbackUrl,
                        leaseSeconds = subscriptionRequest.leaseSeconds
                    )

                    // Send the subscription request to the YouTube PubSubHubbub hub
                    val hubSuccess = pubSubHubbubService.sendSubscriptionRequest(
                        topic = subscriptionRequest.topic,
                        callback = callbackUrl,
                        leaseSeconds = subscriptionRequest.leaseSeconds
                    )

                    if (hubSuccess) {
                        logger.info("Successfully sent subscription request to hub for channel: ${subscriptionRequest.channelId}")
                        call.respond(HttpStatusCode.Created, subscription)
                    } else {
                        // Update subscription status to indicate the hub request failed
                        subscriptionRepository.updateSubscriptionStatus(subscriptionRequest.channelId, "pending")
                        logger.warn("Failed to send subscription request to hub for channel: ${subscriptionRequest.channelId}")
                        call.respond(
                            HttpStatusCode.Accepted, 
                            mapOf(
                                "subscription" to subscription,
                                "hubStatus" to "failed",
                                "message" to "Subscription created but hub request failed. The subscription is in 'pending' state."
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Error creating subscription: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error creating subscription: ${e.message}")
                }
            }

            // Update a subscription
            put("/{channelId}") {
                try {
                    val channelId = call.parameters["channelId"] ?: throw IllegalArgumentException("Missing channelId parameter")
                    val subscriptionRequest = call.receive<SubscriptionRequest>()

                    // Check if the subscription exists
                    val existingSubscription = subscriptionRepository.getSubscription(channelId)
                    if (existingSubscription == null) {
                        call.respond(HttpStatusCode.NotFound, "Subscription not found for channel ID: $channelId")
                        return@put
                    }

                    // Validate request
                    if (subscriptionRequest.topic.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Topic URL is required")
                        return@put
                    }

                    if (subscriptionRequest.callbackUrl.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Callback URL is required")
                        return@put
                    }

                    if (subscriptionRequest.leaseSeconds <= 0) {
                        call.respond(HttpStatusCode.BadRequest, "Lease seconds must be greater than 0")
                        return@put
                    }

                    // Always use our fixed callback URL regardless of what was provided in the request
                    val callbackUrl = System.getenv("CALLBACK_URL") ?: throw IllegalStateException("Missing CALLBACK_URL environment variable")

                    // Update the subscription
                    val subscription = subscriptionRepository.createOrUpdateSubscription(
                        channelId = channelId,
                        topic = subscriptionRequest.topic,
                        callbackUrl = callbackUrl,
                        leaseSeconds = subscriptionRequest.leaseSeconds
                    )

                    call.respond(subscription)
                } catch (e: Exception) {
                    logger.error("Error updating subscription: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error updating subscription: ${e.message}")
                }
            }

            // Update subscription status
            put("/{channelId}/status") {
                try {
                    val channelId = call.parameters["channelId"] ?: throw IllegalArgumentException("Missing channelId parameter")
                    val statusRequest = call.receive<StatusRequest>()

                    // Validate status
                    if (statusRequest.status.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Status is required")
                        return@put
                    }

                    if (statusRequest.status != "active" && statusRequest.status != "inactive") {
                        call.respond(HttpStatusCode.BadRequest, "Status must be 'active' or 'inactive'")
                        return@put
                    }

                    // Update the subscription status
                    val updated = subscriptionRepository.updateSubscriptionStatus(channelId, statusRequest.status)

                    if (updated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Subscription status updated to ${statusRequest.status}"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Subscription not found for channel ID: $channelId")
                    }
                } catch (e: Exception) {
                    logger.error("Error updating subscription status: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error updating subscription status: ${e.message}")
                }
            }

            // Delete a subscription
            delete("/{channelId}") {
                try {
                    val channelId = call.parameters["channelId"] ?: throw IllegalArgumentException("Missing channelId parameter")

                    // Delete the subscription
                    val deleted = subscriptionRepository.deleteSubscription(channelId)

                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Subscription deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Subscription not found for channel ID: $channelId")
                    }
                } catch (e: Exception) {
                    logger.error("Error deleting subscription: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error deleting subscription: ${e.message}")
                }
            }
        }
    }
}
