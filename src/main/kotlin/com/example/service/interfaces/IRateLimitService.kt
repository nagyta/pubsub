package com.example.service.interfaces

import io.ktor.server.application.ApplicationCall

/**
 * Interface for rate limiting operations.
 * This interface defines the contract for rate limit services.
 */
interface IRateLimitService {
    /**
     * Initialize the rate limit service.
     */
    fun init()

    /**
     * Check if a request should be rate limited.
     * 
     * @param call The ApplicationCall to check
     * @return True if the request is allowed, false if it should be rate limited
     */
    fun checkRateLimit(call: ApplicationCall): Boolean

    /**
     * Get the current rate limit configuration.
     *
     * @return A map containing the current configuration
     */
    fun getConfiguration(): Map<String, Any>

    /**
     * Update the rate limit configuration.
     *
     * @param enabled Whether rate limiting is enabled
     * @param defaultLimit The default rate limit (requests per minute)
     * @param apiLimit The rate limit for API endpoints (requests per minute)
     * @param pubsubLimit The rate limit for PubSub endpoints (requests per minute)
     * @param windowSize The window size in seconds
     * @return A map containing the updated configuration
     */
    fun updateConfiguration(
        enabled: Boolean,
        defaultLimit: Int,
        apiLimit: Int,
        pubsubLimit: Int,
        windowSize: Int
    ): Map<String, Any>
}
