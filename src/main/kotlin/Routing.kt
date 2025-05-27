package com.example

import com.example.models.AtomFeed
import com.example.models.ConfigRequest
import com.example.models.StatusRequest
import com.example.models.SubscriptionEntity
import com.example.models.SubscriptionRequest
import com.example.models.toNotification
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Routing configuration for the YouTube PubSubHubbub Service.
 *
 * This file defines the endpoints for handling PubSubHubbub subscription verification and content notifications.
 * The service implements the subscriber role in the PubSubHubbub protocol, receiving notifications from YouTube
 * when new videos are published by subscribed channels.
 *
 * PubSubHubbub Protocol Flow:
 * 1. A subscriber (this service) subscribes to a topic (YouTube channel) via the hub (YouTube's PubSubHubbub endpoint)
 * 2. The hub verifies the subscription by sending a GET request with a challenge
 * 3. When new content is available, the hub sends a POST request with the content details
 *
 * Reference: https://pubsubhubbub.github.io/PubSubHubbub/pubsubhubbub-core-0.4.html
 */

private val logger = LoggerFactory.getLogger("com.example.Routing")

fun Application.configureRouting() {
    routing {
        // Home page
        get("/") {
            call.respondText("YouTube PubSubHubbub Service")
        }

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

                if (allServicesAvailable) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status" to "READY",
                        "instance" to (System.getenv("INSTANCE_ID") ?: "unknown"),
                        "timestamp" to System.currentTimeMillis()
                    ))
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                        "status" to "NOT_READY",
                        "instance" to (System.getenv("INSTANCE_ID") ?: "unknown"),
                        "timestamp" to System.currentTimeMillis()
                    ))
                }
            }
        }

        // PubSubHubbub endpoint
        route("/pubsub/youtube") {
            /**
             * Handle subscription verification (GET request)
             * 
             * When a subscription is created or renewed, YouTube sends a GET request to verify the subscriber.
             * This request includes a 'hub.challenge' parameter that must be echoed back to confirm the subscription.
             * 
             * Parameters:
             * - hub.challenge: A random string that must be echoed back to confirm the subscription
             * - hub.mode: The mode of the request (subscribe or unsubscribe)
             * - hub.topic: The topic URL being subscribed to
             * - hub.lease_seconds: The number of seconds for which the subscription is valid
             * 
             * Reference: https://pubsubhubbub.github.io/PubSubHubbub/pubsubhubbub-core-0.4.html#verifysub
             */
            get {
                // YouTube sends a GET request with hub.challenge when verifying a subscription
                val hubChallenge = call.request.queryParameters["hub.challenge"]
                val hubMode = call.request.queryParameters["hub.mode"]
                val hubTopic = call.request.queryParameters["hub.topic"]
                val hubLeaseSeconds = call.request.queryParameters["hub.lease_seconds"]?.toIntOrNull() ?: 0

                if (hubChallenge != null) {
                    logger.info("Received subscription verification with challenge: $hubChallenge")

                    // Store subscription information if this is a subscription request
                    if (hubMode == "subscribe" && hubTopic != null && hubLeaseSeconds > 0) {
                        try {
                            // Extract channel ID from topic URL
                            // Example topic URL: https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw
                            val channelId = hubTopic.substringAfter("channel_id=")

                            // Store subscription in database
                            val callbackUrl = "http://pubsub.wernernagy.hu/pubsub/youtube"
                            subscriptionRepository.createOrUpdateSubscription(
                                channelId = channelId,
                                topic = hubTopic,
                                callbackUrl = callbackUrl,
                                leaseSeconds = hubLeaseSeconds
                            )

                            logger.info("Subscription stored for channel: $channelId with lease: $hubLeaseSeconds seconds")
                        } catch (e: Exception) {
                            logger.error("Error storing subscription: ${e.message}", e)
                        }
                    } else if (hubMode == "unsubscribe" && hubTopic != null) {
                        try {
                            // Extract channel ID from topic URL
                            val channelId = hubTopic.substringAfter("channel_id=")

                            // Update subscription status to inactive
                            subscriptionRepository.updateSubscriptionStatus(channelId, "inactive")

                            logger.info("Subscription marked as inactive for channel: $channelId")
                        } catch (e: Exception) {
                            logger.error("Error updating subscription status: ${e.message}", e)
                        }
                    }

                    // Respond with the challenge to confirm the subscription
                    call.respondText(hubChallenge)
                } else {
                    logger.warn("Received GET request without hub.challenge")
                    call.respond(HttpStatusCode.BadRequest, "Missing hub.challenge parameter")
                }
            }

            /**
             * Handle content notifications (POST request)
             * 
             * When new content is available, YouTube sends a POST request with the content details.
             * The request body contains an Atom feed with information about the new video.
             * 
             * Validation:
             * - The feed must contain an entry element
             * - The entry must have a non-empty title
             * - The entry must have a non-empty video ID
             * 
             * Reference: https://pubsubhubbub.github.io/PubSubHubbub/pubsubhubbub-core-0.4.html#contentdist
             */
            post {
                try {
                    // Parse the feed from the request body
                    val feed = call.receive<AtomFeed>()

                    // Validate the feed structure
                    if (feed.entry == null) {
                        logger.warn("Received notification without entry element")
                        call.respond(HttpStatusCode.BadRequest, "Missing entry element in feed")
                        return@post
                    }

                    // Extract and validate required fields
                    val entry = feed.entry
                    val title = entry.title
                    val videoId = entry.id
                    val channelName = entry.author?.name
                    val channelUri = entry.author?.uri

                    // Validate title
                    if (title.isNullOrBlank()) {
                        logger.warn("Received notification with empty or missing title")
                        call.respond(HttpStatusCode.BadRequest, "Missing or empty title in feed")
                        return@post
                    }

                    // Validate video ID
                    if (videoId.isNullOrBlank()) {
                        logger.warn("Received notification with empty or missing video ID")
                        call.respond(HttpStatusCode.BadRequest, "Missing or empty video ID in feed")
                        return@post
                    }

                    // Extract channel ID from channel URI if available
                    val channelId = channelUri?.substringAfterLast("/")

                    // Verify that we have an active subscription for this channel
                    if (channelId != null) {
                        val subscription = subscriptionRepository.getSubscription(channelId)
                        if (subscription == null || subscription.status != "active") {
                            logger.warn("Received notification for channel without active subscription: $channelId")
                        }
                    }

                    // Log the notification with structured information
                    logger.info("New YouTube content: '$title' by $channelName (ID: $videoId)")

                    // Additional details for debugging
                    logger.debug("Feed details - Published: ${entry.published}, Updated: ${entry.updated}, " +
                                "Links: ${entry.links?.size ?: 0}")

                    // Queue the notification for processing
                    try {
                        val notification = entry.toNotification()
                        val queued = notificationQueueService.queueNotification(notification)

                        if (queued) {
                            logger.info("Notification queued successfully: $title")
                        } else {
                            logger.warn("Failed to queue notification: $title")
                        }
                    } catch (e: Exception) {
                        logger.error("Error queueing notification: ${e.message}", e)
                    }

                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    logger.error("Error processing notification: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, "Invalid request format: ${e.message}")
                }
            }
        }

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
                    val callbackUrl = "http://pubsub.wernernagy.hu/pubsub/youtube"

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

                    // Check if subscription exists
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
                    val callbackUrl = "http://pubsub.wernernagy.hu/pubsub/youtube"

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

        // Service configuration management endpoints (Phase 4)
        route("/api/config") {
            // Get current service configuration
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
