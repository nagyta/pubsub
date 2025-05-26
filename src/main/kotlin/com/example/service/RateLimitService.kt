package com.example.service

import com.example.cacheService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import java.time.Instant
import org.slf4j.LoggerFactory

/**
 * Service for rate limiting requests to protect against DoS attacks and excessive resource usage.
 * Implements both IP-based and endpoint-based rate limiting.
 */
class RateLimitService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val cacheName = "rate_limits"
    
    // Default rate limits (requests per minute)
    private val defaultLimit = 60
    private val apiLimit = 30
    private val pubsubLimit = 120  // Higher limit for YouTube notifications
    
    // Window size in seconds
    private val windowSize = 60
    
    /**
     * Initialize the rate limit service.
     */
    fun init() {
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
    suspend fun checkRateLimit(call: ApplicationCall): Boolean {
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
     * Data class to store rate limiting information.
     */
    data class RateLimitData(
        var count: Int = 0,
        var timestamp: Long = Instant.now().epochSecond
    )
}
