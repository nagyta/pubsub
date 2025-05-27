package com.example.service

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.slf4j.LoggerFactory

/**
 * Service for caching data using Ehcache.
 */
class CacheService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var cacheManager: CacheManager? = null

    // Configuration properties
    private var enabled = AtomicBoolean(true)
    private var heapSize = 100
    private var ttlMinutes = 10

    /**
     * Initialize the cache manager.
     */
    fun init() {
        try {
            // If caching is disabled, don't initialize
            if (!enabled.get()) {
                logger.info("Cache is disabled, skipping initialization")
                return
            }

            logger.info("Initializing cache manager with heapSize=$heapSize, ttlMinutes=$ttlMinutes")

            // Close existing cache manager if it exists
            cacheManager?.close()

            cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .build(true)

            // Create subscription cache
            cacheManager?.createCache("subscriptions", 
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String::class.java,
                    Any::class.java,
                    ResourcePoolsBuilder.heap(heapSize.toLong())
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(ttlMinutes.toLong())))
                .build()
            )

            // Create rate limits cache with shorter TTL
            cacheManager?.createCache("rate_limits", 
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String::class.java,
                    Any::class.java,
                    ResourcePoolsBuilder.heap(heapSize.toLong())
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(2)))
                .build()
            )

            logger.info("Cache manager initialized")
        } catch (e: Exception) {
            logger.error("Error initializing cache manager: ${e.message}", e)
        }
    }

    /**
     * Get the current cache configuration.
     *
     * @return A map containing the current configuration
     */
    fun getConfiguration(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled.get(),
            "heapSize" to heapSize,
            "ttlMinutes" to ttlMinutes
        )
    }

    /**
     * Update the cache configuration.
     * This will reinitialize the cache with the new configuration.
     *
     * @param enabled Whether caching is enabled
     * @param heapSize The number of entries to store in the heap
     * @param ttlMinutes The time-to-live in minutes for cache entries
     * @return A map containing the updated configuration
     */
    fun updateConfiguration(enabled: Boolean, heapSize: Int, ttlMinutes: Int): Map<String, Any> {
        logger.info("Updating cache configuration: enabled=$enabled, heapSize=$heapSize, ttlMinutes=$ttlMinutes")

        this.enabled.set(enabled)
        this.heapSize = heapSize
        this.ttlMinutes = ttlMinutes

        // Reinitialize the cache with the new configuration
        init()

        return getConfiguration()
    }

    /**
     * Get a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to get
     * @return The value or null if not found
     */
    fun <T> get(cacheName: String, key: String): T? {
        return try {
            cacheManager?.getCache(cacheName, String::class.java, Any::class.java)?.get(key) as? T
        } catch (e: Exception) {
            logger.error("Error getting from cache: ${e.message}", e)
            null
        }
    }

    /**
     * Put a value in the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to put
     * @param value The value to put
     */
    fun put(cacheName: String, key: String, value: Any) {
        try {
            cacheManager?.getCache(cacheName, String::class.java, Any::class.java)?.put(key, value)
        } catch (e: Exception) {
            logger.error("Error putting in cache: ${e.message}", e)
        }
    }

    /**
     * Remove a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to remove
     */
    fun remove(cacheName: String, key: String) {
        try {
            cacheManager?.getCache(cacheName, String::class.java, Any::class.java)?.remove(key)
        } catch (e: Exception) {
            logger.error("Error removing from cache: ${e.message}", e)
        }
    }

    /**
     * Clear a cache.
     *
     * @param cacheName The name of the cache to clear
     */
    fun clear(cacheName: String) {
        try {
            cacheManager?.getCache(cacheName, String::class.java, Any::class.java)?.clear()
        } catch (e: Exception) {
            logger.error("Error clearing cache: ${e.message}", e)
        }
    }

    /**
     * Close the cache manager.
     */
    fun close() {
        try {
            cacheManager?.close()
            logger.info("Cache manager closed")
        } catch (e: Exception) {
            logger.error("Error closing cache manager: ${e.message}", e)
        }
    }

    /**
     * Check if the cache is available.
     *
     * @return True if the cache is available, false otherwise
     */
    fun isAvailable(): Boolean {
        return try {
            if (cacheManager == null) {
                logger.warn("Cache manager is not initialized, attempting to initialize")
                init()
            }

            // Try to get a cache to verify it's working
            val cache = cacheManager?.getCache("subscriptions", String::class.java, Any::class.java)
            cache != null
        } catch (e: Exception) {
            logger.error("Error checking cache availability: ${e.message}", e)
            false
        }
    }
}
