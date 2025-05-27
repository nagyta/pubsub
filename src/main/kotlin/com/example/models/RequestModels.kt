package com.example.models

/**
 * Request models for the management API.
 * These classes represent the data received in API requests.
 */

/**
 * Request model for creating or updating a subscription
 */
data class SubscriptionRequest(
    val channelId: String,
    val topic: String = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId",
    val callbackUrl: String = "http://pubsub.wernernagy.hu/pubsub/youtube",
    val leaseSeconds: Int = 3600,
)

/**
 * Request model for updating a subscription's status
 */
data class StatusRequest(
    val status: String
)

/**
 * Request model for service configuration
 */
data class ConfigRequest(
    val cacheEnabled: Boolean,
    val cacheHeapSize: Int,
    val cacheTtlSeconds: Int,
    val rateLimitEnabled: Boolean,
    val rateLimitPerMinute: Int
)
