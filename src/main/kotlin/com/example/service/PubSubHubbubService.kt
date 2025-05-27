package com.example.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Service for interacting with the YouTube PubSubHubbub hub.
 * This service handles sending subscription requests to the hub.
 */
class PubSubHubbubService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val client = HttpClient(CIO) {
        expectSuccess = false
        followRedirects = false
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        engine {
            requestTimeout = 30000
        }
    }
    private val hubUrl = "https://pubsubhubbub.appspot.com/subscribe"

    /**
     * Send a subscription request to the YouTube PubSubHubbub hub.
     *
     * @param topic The topic URL (YouTube channel feed URL)
     * @param callback The callback URL where notifications should be sent
     * @param leaseSeconds The number of seconds for which the subscription is valid
     * @param mode The subscription mode ("subscribe" or "unsubscribe")
     * @return True if the request was successful, false otherwise
     */
    suspend fun sendSubscriptionRequest(
        topic: String,
        callback: String,
        leaseSeconds: Int,
        mode: String = "subscribe"
    ): Boolean {
        return try {
            logger.info("Sending $mode request to hub for topic: $topic")

            val response = withContext(Dispatchers.IO) {
                client.post(hubUrl) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    parameters {
                        parameter("hub.callback", callback)
                        parameter("hub.topic", topic)
                        parameter("hub.verify", "sync")
                        parameter("hub.mode", mode)
                        parameter("hub.lease_seconds", leaseSeconds.toString())
                    }
                }
            }

            val success = response.status.isSuccess()
            if (success) {
                logger.info("Successfully sent $mode request to hub for topic: $topic")
            } else {
                logger.error("Failed to send $mode request to hub for topic: $topic. Status: ${response.status}")
            }

            success
        } catch (e: Exception) {
            logger.error("Error sending $mode request to hub for topic: $topic: ${e.message}", e)
            false
        }
    }

    /**
     * Close the HTTP client when the service is no longer needed.
     */
    fun close() {
        client.close()
    }
}
