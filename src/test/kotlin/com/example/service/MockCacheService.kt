package com.example.service

import com.example.service.interfaces.ICacheService
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock implementation of ICacheService for testing.
 */
class MockCacheService : ICacheService {
    // Use ConcurrentHashMap to store cache data
    private val caches = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()
    private var enabled = true
    private var heapSize = 100
    private var ttlMinutes = 10

    /**
     * Initialize the cache.
     */
    override fun init() {
        // No-op for mock
    }

    /**
     * Get a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to get
     * @return The value or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(cacheName: String, key: String): T? {
        return caches.getOrPut(cacheName) { ConcurrentHashMap() }[key] as? T
    }

    /**
     * Put a value in the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to put
     * @param value The value to put
     */
    override fun put(cacheName: String, key: String, value: Any) {
        caches.getOrPut(cacheName) { ConcurrentHashMap() }[key] = value
    }

    /**
     * Remove a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to remove
     */
    override fun remove(cacheName: String, key: String) {
        caches[cacheName]?.remove(key)
    }

    /**
     * Clear a cache.
     *
     * @param cacheName The name of the cache to clear
     */
    override fun clear(cacheName: String) {
        caches[cacheName]?.clear()
    }

    /**
     * Close the cache.
     */
    override fun close() {
        caches.clear()
    }

    /**
     * Check if the cache is available.
     *
     * @return True if the cache is available, false otherwise
     */
    override fun isAvailable(): Boolean {
        return enabled
    }

    /**
     * Get the current cache configuration.
     *
     * @return A map containing the current configuration
     */
    override fun getConfiguration(): Map<String, String> {
        return mapOf(
            "enabled" to enabled.toString(),
            "heapSize" to heapSize.toString(),
            "ttlMinutes" to ttlMinutes.toString()
        )
    }

    /**
     * Update the cache configuration.
     *
     * @param enabled Whether caching is enabled
     * @param heapSize The number of entries to store in the heap
     * @param ttlMinutes The time-to-live in minutes for cache entries
     * @return A map containing the updated configuration
     */
    override fun updateConfiguration(enabled: Boolean, heapSize: Int, ttlMinutes: Int): Map<String, String> {
        this.enabled = enabled
        this.heapSize = heapSize
        this.ttlMinutes = ttlMinutes
        return getConfiguration()
    }
}
