package com.example.service.interfaces

/**
 * Interface for cache operations.
 * This interface defines the contract for cache services.
 */
interface ICacheService {
    /**
     * Initialize the cache.
     */
    fun init()

    /**
     * Get a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to get
     * @return The value or null if not found
     */
    fun <T> get(cacheName: String, key: String): T?

    /**
     * Put a value in the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to put
     * @param value The value to put
     */
    fun put(cacheName: String, key: String, value: Any)

    /**
     * Remove a value from the cache.
     *
     * @param cacheName The name of the cache
     * @param key The key to remove
     */
    fun remove(cacheName: String, key: String)

    /**
     * Clear a cache.
     *
     * @param cacheName The name of the cache to clear
     */
    fun clear(cacheName: String)

    /**
     * Close the cache.
     */
    fun close()

    /**
     * Check if the cache is available.
     *
     * @return True if the cache is available, false otherwise
     */
    fun isAvailable(): Boolean

    /**
     * Get the current cache configuration.
     *
     * @return A map containing the current configuration
     */
    fun getConfiguration(): Map<String, Any>

    /**
     * Update the cache configuration.
     *
     * @param enabled Whether caching is enabled
     * @param heapSize The number of entries to store in the heap
     * @param ttlMinutes The time-to-live in minutes for cache entries
     * @return A map containing the updated configuration
     */
    fun updateConfiguration(enabled: Boolean, heapSize: Int, ttlMinutes: Int): Map<String, Any>
}
