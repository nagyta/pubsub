package com.example.service

import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Service for caching data using Ehcache.
 */
class CacheService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var cacheManager: CacheManager? = null
    
    /**
     * Initialize the cache manager.
     */
    fun init() {
        try {
            logger.info("Initializing cache manager")
            
            cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .build(true)
            
            // Create subscription cache
            cacheManager?.createCache("subscriptions", 
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String::class.java,
                    Any::class.java,
                    ResourcePoolsBuilder.heap(100)
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))
                .build()
            )
            
            logger.info("Cache manager initialized")
        } catch (e: Exception) {
            logger.error("Error initializing cache manager: ${e.message}", e)
        }
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
}
