package com.example

import com.example.models.AtomFeed
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.Routing")

fun Application.configureRouting() {
    routing {
        // Home page
        get("/") {
            call.respondText("YouTube PubSubHubbub Service")
        }

        // PubSubHubbub endpoint
        route("/pubsub/youtube") {
            // Handle subscription verification (GET request)
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

            // Handle content notifications (POST request)
            post {
                try {
                    // Parse the XML feed from the request body
                    val feed = call.receive<AtomFeed>()

                    // Extract and log the video title
                    val title = feed.entry?.title
                    val videoId = feed.entry?.id
                    val channelName = feed.entry?.author?.name

                    if (title != null) {
                        logger.info("New YouTube content: '$title' by $channelName (ID: $videoId)")
                        call.respond(HttpStatusCode.OK)
                    } else {
                        logger.warn("Received notification without title")
                        call.respond(HttpStatusCode.BadRequest, "Missing title in feed")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing notification", e)
                    call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                }
            }
        }
    }
}
