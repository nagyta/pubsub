package com.example

import com.example.models.AtomFeed
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

                if (hubChallenge != null) {
                    logger.info("Received subscription verification with challenge: $hubChallenge")
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

                    // Log the notification with structured information
                    logger.info("New YouTube content: '$title' by $channelName (ID: $videoId)")

                    // Additional details for debugging
                    logger.debug("Feed details - Published: ${entry.published}, Updated: ${entry.updated}, " +
                                "Links: ${entry.links?.size ?: 0}")

                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    logger.error("Error processing notification: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, "Invalid request format: ${e.message}")
                }
            }
        }
    }
}
