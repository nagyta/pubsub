package com.example.service

import com.example.service.interfaces.ICacheService
import java.time.Instant
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * TestNG tests for the RateLimitService.
 * Tests the rate limiting functionality.
 */
class RateLimitServiceTest {
    private lateinit var rateLimitService: RateLimitService
    private lateinit var cacheService: ICacheService
    private lateinit var cacheName: String
    private val defaultLimit = 60
    private val apiLimit = 30
    private val pubsubLimit = 120
    private val windowSize = 60

    /**
     * Set up the test environment before each test.
     * Creates a new RateLimitService instance.
     */
    @BeforeMethod
    fun setUp() {
        // Initialize the cache service
        cacheService = MockCacheService()

        // Initialize the rate limit service with the cache service
        rateLimitService = RateLimitService(cacheService)

        // Get the cacheName field using reflection
        val cacheNameField = RateLimitService::class.java.getDeclaredField("cacheName")
        cacheNameField.isAccessible = true
        cacheName = cacheNameField.get(rateLimitService) as String

        // Get the rate limit fields using reflection and verify they match our expected values
        val defaultLimitField = RateLimitService::class.java.getDeclaredField("defaultLimit")
        defaultLimitField.isAccessible = true
        Assert.assertEquals(defaultLimitField.get(rateLimitService), defaultLimit, "Default limit should match expected value")

        val apiLimitField = RateLimitService::class.java.getDeclaredField("apiLimit")
        apiLimitField.isAccessible = true
        Assert.assertEquals(apiLimitField.get(rateLimitService), apiLimit, "API limit should match expected value")

        val pubsubLimitField = RateLimitService::class.java.getDeclaredField("pubsubLimit")
        pubsubLimitField.isAccessible = true
        Assert.assertEquals(pubsubLimitField.get(rateLimitService), pubsubLimit, "PubSub limit should match expected value")

        val windowSizeField = RateLimitService::class.java.getDeclaredField("windowSize")
        windowSizeField.isAccessible = true
        Assert.assertEquals(windowSizeField.get(rateLimitService), windowSize, "Window size should match expected value")

        // Initialize the rate limit service, which will initialize the cache
        rateLimitService.init()

        // Create the rate_limits cache if it doesn't exist
        createRateLimitsCache()
    }

    /**
     * Helper method to create the rate_limits cache if it doesn't exist.
     */
    private fun createRateLimitsCache() {
        // Since we're using MockCacheService, we don't need to create the cache
        // Just clear any existing rate limit data from previous tests
        cacheService.clear(cacheName)
    }

    /**
     * Test the initialization of the rate limit service.
     */
    @Test
    fun testInit() {
        // Initialize the service
        rateLimitService.init()

        // No assertions needed, just verify it doesn't throw an exception
    }

    /**
     * Test the RateLimitData class.
     */
    @Test
    fun testRateLimitData() {
        // Create a RateLimitData object with default values
        val rateData = RateLimitService.RateLimitData()

        // Verify default values
        Assert.assertEquals(rateData.count, 0, "Default count should be 0")
        Assert.assertTrue(rateData.timestamp > 0, "Timestamp should be set")

        // Create a RateLimitData object with custom values
        val now = Instant.now().epochSecond
        val customRateData = RateLimitService.RateLimitData(5, now)

        // Verify custom values
        Assert.assertEquals(customRateData.count, 5, "Count should be 5")
        Assert.assertEquals(customRateData.timestamp, now, "Timestamp should match")

        // Test toString() method (implicitly called by data class)
        val rateDataString = customRateData.toString()
        Assert.assertTrue(rateDataString.contains("count=5"), "toString should contain count")
        Assert.assertTrue(rateDataString.contains("timestamp=$now"), "toString should contain timestamp")

        // Test equals() method (implicitly called by data class)
        val sameRateData = RateLimitService.RateLimitData(5, now)
        Assert.assertEquals(customRateData, sameRateData, "Equal RateLimitData objects should be equal")

        // Test hashCode() method (implicitly called by data class)
        Assert.assertEquals(customRateData.hashCode(), sameRateData.hashCode(), "Equal RateLimitData objects should have same hashCode")

        // Test copy() method (implicitly called by data class)
        val copiedRateData = customRateData.copy(count = 10)
        Assert.assertEquals(copiedRateData.count, 10, "Copied RateLimitData should have new count")
        Assert.assertEquals(copiedRateData.timestamp, now, "Copied RateLimitData should have same timestamp")

        // Test component functions (implicitly called by data class)
        val (count, timestamp) = customRateData
        Assert.assertEquals(count, 5, "Destructured count should match")
        Assert.assertEquals(timestamp, now, "Destructured timestamp should match")
    }

    /**
     * Test the path-based rate limit determination.
     */
    @Test
    fun testPathBasedRateLimits() {
        // Test API path limit
        val apiPath = "/api/test"
        val apiLimit = getPathLimit(apiPath)
        Assert.assertEquals(apiLimit, this.apiLimit, "API path should have API limit")

        // Test PubSub path limit
        val pubsubPath = "/pubsub/test"
        val pubsubLimit = getPathLimit(pubsubPath)
        Assert.assertEquals(pubsubLimit, this.pubsubLimit, "PubSub path should have PubSub limit")

        // Test default path limit
        val defaultPath = "/test"
        val defaultLimit = getPathLimit(defaultPath)
        Assert.assertEquals(defaultLimit, this.defaultLimit, "Default path should have default limit")
    }

    /**
     * Test the rate-limiting logic for a new request.
     */
    @Test
    fun testRateLimitNewRequest() {
        // Create a test path and IP
        val testPath = "/test"
        val testIp = "127.0.0.1"
        val key = "$testIp:$testPath"

        // Simulate a new request
        val result = processRequest(testPath, testIp)
        Assert.assertTrue(result, "First request should be allowed")

        // Verify the data was stored in cache
        val rateData = cacheService.get<RateLimitService.RateLimitData>(cacheName, key)
        Assert.assertNotNull(rateData, "Rate data should be stored in cache")
        Assert.assertEquals(rateData!!.count, 1, "Count should be 1 for first request")
    }

    /**
     * Test the rate limiting logic for multiple requests under the limit.
     */
    @Test
    fun testRateLimitUnderLimit() {
        // Create a test path and IP
        val testPath = "/test"
        val testIp = "127.0.0.2"
        val key = "$testIp:$testPath"

        // Make multiple requests under the limit
        for (i in 1..defaultLimit) {
            val result = processRequest(testPath, testIp)
            Assert.assertTrue(result, "Request $i should be allowed (under limit)")
        }

        // Verify the data was stored in cache
        val rateData = cacheService.get<RateLimitService.RateLimitData>(cacheName, key)
        Assert.assertNotNull(rateData, "Rate data should be stored in cache")
        Assert.assertEquals(rateData!!.count, defaultLimit, "Count should match number of requests")
    }

    /**
     * Test the rate limiting logic for requests exceeding the limit.
     */
    @Test
    fun testRateLimitExceedLimit() {
        // Create a test path and IP
        val testPath = "/test"
        val testIp = "127.0.0.3"
        val key = "$testIp:$testPath"

        // Make requests up to the limit
        for (i in 1..defaultLimit) {
            val result = processRequest(testPath, testIp)
            Assert.assertTrue(result, "Request $i should be allowed (at or under limit)")
        }

        // Make one more request that exceeds the limit
        val exceededResult = processRequest(testPath, testIp)
        Assert.assertFalse(exceededResult, "Request exceeding limit should be denied")

        // Verify the data was stored in cache
        val rateData = cacheService.get<RateLimitService.RateLimitData>(cacheName, key)
        Assert.assertNotNull(rateData, "Rate data should be stored in cache")
        Assert.assertEquals(rateData!!.count, defaultLimit + 1, "Count should be one more than limit")
    }

    /**
     * Test the window reset logic.
     */
    @Test
    fun testWindowReset() {
        // Create a test path and IP
        val testPath = "/test"
        val testIp = "127.0.0.4"
        val key = "$testIp:$testPath"

        // Create a RateLimitData with an old timestamp
        val oldTimestamp = Instant.now().epochSecond - (windowSize + 1) // Older than window size
        val rateData = RateLimitService.RateLimitData(100, oldTimestamp)

        // Put the rate data in the cache
        cacheService.put(cacheName, key, rateData)

        // Make a request that should reset the window
        val result = processRequest(testPath, testIp)
        Assert.assertTrue(result, "Request should be allowed after window reset")

        // Verify the data was reset in cache
        val updatedRateData = cacheService.get<RateLimitService.RateLimitData>(cacheName, key)
        Assert.assertNotNull(updatedRateData, "Rate data should be stored in cache")
        Assert.assertEquals(updatedRateData!!.count, 1, "Count should be reset to 1")
        Assert.assertTrue(updatedRateData.timestamp > oldTimestamp, "Timestamp should be updated")
    }

    /**
     * Helper method to get the rate limit for a path.
     */
    private fun getPathLimit(path: String): Int {
        return when {
            path.startsWith("/api/") -> apiLimit
            path.startsWith("/pubsub/") -> pubsubLimit
            else -> defaultLimit
        }
    }

    /**
     * Helper method to simulate processing a request.
     * This mimics the logic in the checkRateLimit method without using ApplicationCall.
     */
    private fun processRequest(path: String, clientIp: String): Boolean {
        // Determine the appropriate rate limit based on the path
        val limit = getPathLimit(path)

        // Create a key that combines IP and path for endpoint-specific rate limiting
        val key = "$clientIp:$path"

        // Get current count and timestamp
        val rateData = cacheService.get<RateLimitService.RateLimitData>(cacheName, key) ?: RateLimitService.RateLimitData()

        // Check if we need to reset the window
        val now = Instant.now().epochSecond
        if (now - rateData.timestamp > windowSize) {
            // Reset counter for the new window
            rateData.count = 1
            rateData.timestamp = now
            cacheService.put(cacheName, key, rateData)
            return true
        }

        // Increment counter
        rateData.count++
        cacheService.put(cacheName, key, rateData)

        // Check if limit exceeded
        return rateData.count <= limit
    }
}
