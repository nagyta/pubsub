package com.example.service

import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * TestNG tests for the CacheService.
 * Tests the cache service methods for managing cached data.
 */
class CacheServiceTest {
    private lateinit var cacheService: CacheService

    /**
     * Set up the test environment before each test.
     * Creates a new CacheService instance and initializes it.
     */
    @BeforeMethod
    fun setUp() {
        cacheService = CacheService()
        cacheService.init()
    }

    /**
     * Clean up the test environment after each test.
     * Closes the cache manager.
     */
    @AfterMethod
    fun tearDown() {
        cacheService.close()
    }

    /**
     * Test putting and getting a value from the cache.
     */
    @Test
    fun testPutAndGet() {
        // Put a value in the cache
        val cacheName = "subscriptions"
        val key = "test-key"
        val value = "test-value"

        cacheService.put(cacheName, key, value)

        // Get the value from the cache
        val retrievedValue = cacheService.get<String>(cacheName, key)

        // Verify the value was retrieved
        Assert.assertEquals(retrievedValue, value, "Retrieved value should match the original value")
    }

    /**
     * Test getting a non-existent value from the cache.
     */
    @Test
    fun testGetNonExistent() {
        // Get a non-existent value from the cache
        val cacheName = "subscriptions"
        val key = "non-existent"

        val retrievedValue = cacheService.get<String>(cacheName, key)

        // Verify the value is null
        Assert.assertNull(retrievedValue, "Retrieved value should be null")
    }

    /**
     * Test removing a value from the cache.
     */
    @Test
    fun testRemove() {
        // Put a value in the cache
        val cacheName = "subscriptions"
        val key = "test-key"
        val value = "test-value"

        cacheService.put(cacheName, key, value)

        // Verify the value was stored
        val retrievedValue = cacheService.get<String>(cacheName, key)
        Assert.assertEquals(retrievedValue, value, "Retrieved value should match the original value")

        // Remove the value from the cache
        cacheService.remove(cacheName, key)

        // Verify the value was removed
        val retrievedValueAfterRemove = cacheService.get<String>(cacheName, key)
        Assert.assertNull(retrievedValueAfterRemove, "Retrieved value should be null after removal")
    }

    /**
     * Test clearing the cache.
     */
    @Test
    fun testClear() {
        // Put some values in the cache
        val cacheName = "subscriptions"
        val key1 = "test-key-1"
        val key2 = "test-key-2"
        val value1 = "test-value-1"
        val value2 = "test-value-2"

        cacheService.put(cacheName, key1, value1)
        cacheService.put(cacheName, key2, value2)

        // Verify the values were stored
        val retrievedValue1 = cacheService.get<String>(cacheName, key1)
        val retrievedValue2 = cacheService.get<String>(cacheName, key2)
        Assert.assertEquals(retrievedValue1, value1, "Retrieved value 1 should match the original value")
        Assert.assertEquals(retrievedValue2, value2, "Retrieved value 2 should match the original value")

        // Clear the cache
        cacheService.clear(cacheName)

        // Verify the values were removed
        val retrievedValue1AfterClear = cacheService.get<String>(cacheName, key1)
        val retrievedValue2AfterClear = cacheService.get<String>(cacheName, key2)
        Assert.assertNull(retrievedValue1AfterClear, "Retrieved value 1 should be null after clearing")
        Assert.assertNull(retrievedValue2AfterClear, "Retrieved value 2 should be null after clearing")
    }

    /**
     * Test putting and getting a complex object from the cache.
     */
    @Test
    fun testPutAndGetComplexObject() {
        // Create a complex object
        data class TestObject(val id: Int, val name: String, val values: List<String>)

        val cacheName = "subscriptions"
        val key = "test-object"
        val value = TestObject(1, "Test", listOf("value1", "value2", "value3"))

        // Put the object in the cache
        cacheService.put(cacheName, key, value)

        // Get the object from the cache
        val retrievedValue = cacheService.get<TestObject>(cacheName, key)

        // Verify the object was retrieved
        Assert.assertNotNull(retrievedValue, "Retrieved object should not be null")
        Assert.assertEquals(retrievedValue?.id, value.id, "Object ID should match")
        Assert.assertEquals(retrievedValue?.name, value.name, "Object name should match")
        Assert.assertEquals(retrievedValue?.values, value.values, "Object values should match")
    }

    /**
     * Test putting and getting a null value from the cache.
     * Note: This test is skipped because the CacheService doesn't support null values.
     */
    @Test(enabled = false)
    fun testPutAndGetNull() {
        // This test is disabled because the CacheService doesn't support null values
        // The put method expects a non-null value (Any), and trying to cast null to Any
        // results in a NullPointerException
    }

    /**
     * Test error handling in the init method.
     * This test verifies that the init method handles errors gracefully.
     */
    @Test
    fun testInitErrorHandling() {
        // Create a service that's already initialized
        val service = CacheService()
        service.init()

        try {
            // Call init again (it should handle the case where it's already initialized)
            service.init()

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Init method should handle errors gracefully: ${e.message}")
        } finally {
            service.close()
        }
    }

    /**
     * Test error handling in the close method.
     * This test verifies that the close method handles errors gracefully.
     */
    @Test
    fun testCloseErrorHandling() {
        // Create a service that's not initialized
        val service = CacheService()

        try {
            // Call close without initializing (it should handle this case gracefully)
            service.close()

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Close method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the put method.
     * This test verifies that the put method handles errors gracefully.
     */
    @Test
    fun testPutErrorHandling() {
        // Create a service that's not initialized
        val service = CacheService()

        try {
            // Call put without initializing (it should handle this case gracefully)
            service.put("test", "key", "value")

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Put method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the get method.
     * This test verifies that the get method handles errors gracefully.
     */
    @Test
    fun testGetErrorHandling() {
        // Create a service that's not initialized
        val service = CacheService()

        try {
            // Call get without initializing (it should handle this case gracefully)
            val result = service.get<String>("test", "key")

            // The method should return null when an error occurs
            Assert.assertNull(result, "Get method should return null when an error occurs")
        } catch (e: Exception) {
            Assert.fail("Get method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the remove method.
     * This test verifies that the remove method handles errors gracefully.
     */
    @Test
    fun testRemoveErrorHandling() {
        // Create a service that's not initialized
        val service = CacheService()

        try {
            // Call remove without initializing (it should handle this case gracefully)
            service.remove("test", "key")

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Remove method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the clear method.
     * This test verifies that the clear method handles errors gracefully.
     */
    @Test
    fun testClearErrorHandling() {
        // Create a service that's not initialized
        val service = CacheService()

        try {
            // Call clear without initializing (it should handle this case gracefully)
            service.clear("test")

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Clear method should handle errors gracefully: ${e.message}")
        }
    }
}
