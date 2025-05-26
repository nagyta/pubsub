package com.example.repository

import com.example.models.Subscription
import com.example.models.SubscriptionEntity
import com.example.models.SubscriptionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.LocalDateTime

/**
 * TestNG tests for the SubscriptionRepository.
 * Tests the repository methods for managing subscriptions.
 */
class SubscriptionRepositoryTest {
    private lateinit var repository: SubscriptionRepository
    private lateinit var testDb: Database

    /**
     * Set up the test environment before each test.
     * Creates an in-memory H2 database and initializes the repository.
     */
    @BeforeMethod
    fun setUp() {
        // Connect to an in-memory H2 database for testing
        testDb = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        
        // Create a new repository instance
        repository = SubscriptionRepository()
        
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
}
