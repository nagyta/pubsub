package com.example

import com.example.models.AtomFeed
import com.example.models.toNotification
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

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
                            val callbackUrl = "${call.request.local.scheme}://${call.request.local.localHost}:${call.request.local.localPort}/pubsub/youtube"
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
             * The request body contains an Atom XML feed with information about the new video.
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
                    // Parse the XML feed from the request body
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

        // Subscription management endpoints
        route("/api/subscriptions") {
            get {
                try {
                    val subscriptions = subscriptionRepository.getAllActiveSubscriptions()
                    call.respond(subscriptions)
                } catch (e: Exception) {
                    logger.error("Error retrieving subscriptions: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving subscriptions: ${e.message}")
                }
            }
        }
    }
}
