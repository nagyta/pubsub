package com.example.service

import com.example.service.interfaces.ICacheService
import com.example.service.interfaces.IRateLimitService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

/**
 * Service for rate limiting requests to protect against DoS attacks and excessive resource usage.
 * Implements both IP-based and endpoint-based rate limiting.
 */
class RateLimitService(private val cacheService: ICacheService) : IRateLimitService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val cacheName = "rate_limits"

    // Configuration properties
    private var enabled = AtomicBoolean(true)

    // Default rate limits (requests per minute)
    private var defaultLimit = 60
    private var apiLimit = 30
    private var pubsubLimit = 120  // Higher limit for YouTube notifications

    // Window size in seconds
    private var windowSize = 60

    /**
     * Initialize the rate limit service.
     */
    override fun init() {
        logger.info("Initializing rate limit service")
        // Ensure the cache is ready for rate limiting
        if (!cacheService.isAvailable()) {
            cacheService.init()
        }
        logger.info("Rate limit service initialized")
    }

    /**
     * Check if a request should be rate limited.
     * 
     * @param call The ApplicationCall to check
     * @return True if the request is allowed, false if it should be rate limited
     */
    override fun checkRateLimit(call: ApplicationCall): Boolean {
        // If rate limiting is disabled, always allow the request
        if (!enabled.get()) {
            return true
        }

        val clientIp = call.request.origin.remoteHost
        val path = call.request.path()

        // Determine the appropriate rate limit based on the path
        val limit = when {
            path.startsWith("/api/") -> apiLimit
            path.startsWith("/pubsub/") -> pubsubLimit
            else -> defaultLimit
        }

        // Create a key that combines IP and path for endpoint-specific rate limiting
        val key = "$clientIp:$path"

        // Get current count and timestamp
        val rateData = cacheService.get<RateLimitData>(cacheName, key) ?: RateLimitData()

        // Check if we need to reset the window
        val now = Instant.now().epochSecond
        if (now - rateData.timestamp > windowSize) {
            // Reset counter for new window
            rateData.count = 1
            rateData.timestamp = now
            cacheService.put(cacheName, key, rateData)
            return true
        }

        // Increment counter
        rateData.count++
        cacheService.put(cacheName, key, rateData)

        // Check if limit exceeded
        if (rateData.count > limit) {
            logger.warn("Rate limit exceeded for $clientIp on $path: ${rateData.count} requests in $windowSize seconds")
            return false
        }

        return true
    }

    /**
     * Get the current rate limit configuration.
     *
     * @return A map containing the current configuration
     */
    override fun getConfiguration(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled.get(),
            "defaultLimit" to defaultLimit,
            "apiLimit" to apiLimit,
            "pubsubLimit" to pubsubLimit,
            "windowSize" to windowSize
        )
    }

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
    override fun updateConfiguration(
        enabled: Boolean,
        defaultLimit: Int,
        apiLimit: Int,
        pubsubLimit: Int,
        windowSize: Int
    ): Map<String, Any> {
        logger.info("Updating rate limit configuration: enabled=$enabled, defaultLimit=$defaultLimit, apiLimit=$apiLimit, pubsubLimit=$pubsubLimit, windowSize=$windowSize")

        this.enabled.set(enabled)
        this.defaultLimit = defaultLimit
        this.apiLimit = apiLimit
        this.pubsubLimit = pubsubLimit
        this.windowSize = windowSize

        return getConfiguration()
    }

    /**
     * Data class to store rate limiting information.
     */
    data class RateLimitData(
        var count: Int = 0,
        var timestamp: Long = Instant.now().epochSecond
    )
}
