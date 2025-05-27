package com.example.service.interfaces

/**
 * Interface for PubSubHubbub operations.
 * This interface defines the contract for PubSubHubbub services.
 */
interface IPubSubHubbubService {
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
    ): Boolean

    /**
     * Close the service when it's no longer needed.
     */
    fun close()
}
