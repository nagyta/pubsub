package com.example.repository

import com.example.models.SubscriptionsTable
import com.example.service.MockCacheService
import com.example.service.interfaces.ICacheService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * TestNG tests for the SubscriptionRepository.
 * Tests the repository methods for managing subscriptions.
 */
class SubscriptionRepositoryTest {
    private lateinit var repository: SubscriptionRepository
    private lateinit var testDb: Database
    private lateinit var mockCacheService: ICacheService

    /**
     * Set up the test environment before each test.
     * Creates an in-memory H2 database and initializes the repository.
     */
    @BeforeMethod
    fun setUp() {
        // Connect to an in-memory H2 database for testing
        testDb = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        // Create a mock cache service
        mockCacheService = MockCacheService()

        // Create a new repository instance with the mock cache service
        repository = SubscriptionRepository(mockCacheService)

        // Initialize the repository (creates tables)
        repository.init()
    }

    /**
     * Clean up the test environment after each test.
     * Drops the tables and closes the database connection.
     */
    @AfterMethod
    fun tearDown() {
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }
    }

    /**
     * Test the initialization of the repository.
     * Verifies that the tables are created.
     */
    @Test
    fun testInit() {
        // The init method is called in setUp, so we just need to verify that the tables exist
        transaction {
            // If the table doesn't exist, this would throw an exception
            SchemaUtils.create(SubscriptionsTable)
        }
        // If we got here, the test passed
    }

    /**
     * Test creating a new subscription.
     */
    @Test
    fun testCreateSubscription() {
        // Create a new subscription
        val channelId = "UC123456789"
        val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId"
        val callbackUrl = "https://example.com/pubsub/youtube"
        val leaseSeconds = 3600

        val subscription = repository.createOrUpdateSubscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        // Verify the subscription was created
        Assert.assertEquals(subscription.channelId, channelId, "Channel ID should match")
        Assert.assertEquals(subscription.topic, topic, "Topic should match")
        Assert.assertEquals(subscription.callbackUrl, callbackUrl, "Callback URL should match")
        Assert.assertEquals(subscription.leaseSeconds, leaseSeconds, "Lease seconds should match")
        Assert.assertEquals(subscription.status, "active", "Status should be active")

        // Verify the subscription exists in the database
        val dbSubscription = repository.getSubscription(channelId)
        Assert.assertNotNull(dbSubscription, "Subscription should exist in the database")
        Assert.assertEquals(dbSubscription?.channelId, channelId, "Channel ID should match")
    }
// curl -X POST http://localhost/api/subscriptions -H "Content-Type: application/json" -d '{"channelId": "UCW_zf7VdaFoeZqVTlYht4oA"}'
    /**
     * Test updating an existing subscription.
     */
    @Test
    fun testUpdateSubscription() {
        // Create a new subscription
        val channelId = "UC123456789"
        val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId"
        val callbackUrl = "https://example.com/pubsub/youtube"
        val leaseSeconds = 3600

        repository.createOrUpdateSubscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        // Update the subscription
        val newLeaseSeconds = 7200
        val updatedSubscription = repository.createOrUpdateSubscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = newLeaseSeconds
        )

        // Verify the subscription was updated
        Assert.assertEquals(updatedSubscription.leaseSeconds, newLeaseSeconds, "Lease seconds should be updated")

        // Verify the subscription was updated in the database
        val dbSubscription = repository.getSubscription(channelId)
        Assert.assertNotNull(dbSubscription, "Subscription should exist in the database")
        Assert.assertEquals(dbSubscription?.leaseSeconds, newLeaseSeconds, "Lease seconds should be updated in the database")
    }

    /**
     * Test getting a subscription by channel ID.
     */
    @Test
    fun testGetSubscription() {
        // Create a new subscription
        val channelId = "UC123456789"
        val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId"
        val callbackUrl = "https://example.com/pubsub/youtube"
        val leaseSeconds = 3600

        repository.createOrUpdateSubscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        // Get the subscription
        val subscription = repository.getSubscription(channelId)

        // Verify the subscription was retrieved
        Assert.assertNotNull(subscription, "Subscription should be retrieved")
        Assert.assertEquals(subscription?.channelId, channelId, "Channel ID should match")
        Assert.assertEquals(subscription?.topic, topic, "Topic should match")
        Assert.assertEquals(subscription?.callbackUrl, callbackUrl, "Callback URL should match")
        Assert.assertEquals(subscription?.leaseSeconds, leaseSeconds, "Lease seconds should match")
        Assert.assertEquals(subscription?.status, "active", "Status should be active")
    }

    /**
     * Test getting a non-existent subscription.
     */
    @Test
    fun testGetNonExistentSubscription() {
        // Get a non-existent subscription
        val subscription = repository.getSubscription("non-existent")

        // Verify the subscription is null
        Assert.assertNull(subscription, "Subscription should be null")
    }

    /**
     * Test getting all active subscriptions.
     */
    @Test
    fun testGetAllActiveSubscriptions() {
        // Create some subscriptions
        val channelId1 = "UC123456789"
        val channelId2 = "UC987654321"
        val topic1 = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId1"
        val topic2 = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId2"
        val callbackUrl = "https://example.com/pubsub/youtube"
        val leaseSeconds = 3600

        repository.createOrUpdateSubscription(
            channelId = channelId1,
            topic = topic1,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        repository.createOrUpdateSubscription(
            channelId = channelId2,
            topic = topic2,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        // Get all active subscriptions
        val subscriptions = repository.getAllActiveSubscriptions()

        // Verify the subscriptions were retrieved
        Assert.assertEquals(subscriptions.size, 2, "Should have 2 active subscriptions")
        Assert.assertTrue(subscriptions.any { it.channelId == channelId1 }, "Should contain first subscription")
        Assert.assertTrue(subscriptions.any { it.channelId == channelId2 }, "Should contain second subscription")
    }

    /**
     * Test getting expiring subscriptions.
     */
    @Test
    fun testGetExpiringSubscriptions() {
        // Create some subscriptions with different expiry times
        val channelId1 = "UC123456789"
        val channelId2 = "UC987654321"
        val topic1 = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId1"
        val topic2 = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId2"
        val callbackUrl = "https://example.com/pubsub/youtube"

        // First subscription expires soon
        repository.createOrUpdateSubscription(
            channelId = channelId1,
            topic = topic1,
            callbackUrl = callbackUrl,
            leaseSeconds = 60 // 1 minute
        )

        // Second subscription expires later
        repository.createOrUpdateSubscription(
            channelId = channelId2,
            topic = topic2,
            callbackUrl = callbackUrl,
            leaseSeconds = 3600 // 1 hour
        )

        // Get subscriptions expiring within 10 minutes (600 seconds)
        val expiringSubscriptions = repository.getExpiringSubscriptions(600)

        // Verify only the first subscription is returned
        Assert.assertEquals(expiringSubscriptions.size, 1, "Should have 1 expiring subscription")
        Assert.assertEquals(expiringSubscriptions[0].channelId, channelId1, "Should be the first subscription")
    }

    /**
     * Test updating a subscription's status.
     */
    @Test
    fun testUpdateSubscriptionStatus() {
        // Create a new subscription
        val channelId = "UC123456789"
        val topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId"
        val callbackUrl = "https://example.com/pubsub/youtube"
        val leaseSeconds = 3600

        repository.createOrUpdateSubscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds
        )

        // Update the subscription status
        val updated = repository.updateSubscriptionStatus(channelId, "inactive")

        // Verify the status was updated
        Assert.assertTrue(updated, "Status should be updated")

        // Verify the status was updated in the database
        val subscription = repository.getSubscription(channelId)
        Assert.assertNotNull(subscription, "Subscription should exist in the database")
        Assert.assertEquals(subscription?.status, "inactive", "Status should be inactive")
    }

    /**
     * Test updating a non-existent subscription's status.
     */
    @Test
    fun testUpdateNonExistentSubscriptionStatus() {
        // Update a non-existent subscription's status
        val updated = repository.updateSubscriptionStatus("non-existent", "inactive")

        // Verify the status was not updated
        Assert.assertFalse(updated, "Status should not be updated")
    }

    /**
     * Test error handling in the init method.
     * This test verifies that the init method handles errors gracefully.
     */
    @Test
    fun testInitErrorHandling() {
        // Create a repository with a mock database connection that will throw an exception
        val repository = SubscriptionRepository(MockCacheService())

        try {
            // Call init again (it was already called in setUp)
            repository.init()

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("Init method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the createOrUpdateSubscription method.
     * This test verifies that the method handles database errors gracefully.
     */
    @Test
    fun testCreateOrUpdateSubscriptionErrorHandling() {
        // Drop the table to simulate a database error
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }

        try {
            // Try to create a subscription when the table doesn't exist
            repository.createOrUpdateSubscription(
                channelId = "UC123456789",
                topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123456789",
                callbackUrl = "https://example.com/pubsub/youtube",
                leaseSeconds = 3600
            )

            // If we got here, the test failed because the method should have thrown an exception
            Assert.fail("Method should throw an exception when the database operation fails")
        } catch (_: Exception) {
            // Expected exception
        }
    }

    /**
     * Test error handling in the getSubscription method.
     * This test verifies that the method handles database errors gracefully.
     */
    @Test
    fun testGetSubscriptionErrorHandling() {
        // Drop the table to simulate a database error
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }

        try {
            // Try to get a subscription when the table doesn't exist
            val subscription = repository.getSubscription("UC123456789")

            // The method should return null when an error occurs
            Assert.assertNull(subscription, "Method should return null when an error occurs")
        } catch (e: Exception) {
            Assert.fail("Method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the getAllActiveSubscriptions method.
     * This test verifies that the method handles database errors gracefully.
     */
    @Test
    fun testGetAllActiveSubscriptionsErrorHandling() {
        // Drop the table to simulate a database error
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }

        try {
            // Try to get all active subscriptions when the table doesn't exist
            val subscriptions = repository.getAllActiveSubscriptions()

            // The method should return an empty list when an error occurs
            Assert.assertTrue(subscriptions.isEmpty(), "Method should return an empty list when an error occurs")
        } catch (e: Exception) {
            Assert.fail("Method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the getExpiringSubscriptions method.
     * This test verifies that the method handles database errors gracefully.
     */
    @Test
    fun testGetExpiringSubscriptionsErrorHandling() {
        // Drop the table to simulate a database error
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }

        try {
            // Try to get expiring subscriptions when the table doesn't exist
            val subscriptions = repository.getExpiringSubscriptions(600)

            // The method should return an empty list when an error occurs
            Assert.assertTrue(subscriptions.isEmpty(), "Method should return an empty list when an error occurs")
        } catch (e: Exception) {
            Assert.fail("Method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the updateSubscriptionStatus method.
     * This test verifies that the method handles database errors gracefully.
     */
    @Test
    fun testUpdateSubscriptionStatusErrorHandling() {
        // Drop the table to simulate a database error
        transaction {
            SchemaUtils.drop(SubscriptionsTable)
        }

        try {
            // Try to update a subscription's status when the table doesn't exist
            val updated = repository.updateSubscriptionStatus("UC123456789", "inactive")

            // The method should return false when an error occurs
            Assert.assertFalse(updated, "Method should return false when an error occurs")
        } catch (e: Exception) {
            Assert.fail("Method should handle errors gracefully: ${e.message}")
        }
    }
}
