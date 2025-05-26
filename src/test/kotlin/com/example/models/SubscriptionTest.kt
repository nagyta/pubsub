package com.example.models

import org.testng.annotations.Test
import org.testng.Assert
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID

/**
 * TestNG tests for the Subscription model.
 * Tests the Subscription data class, SubscriptionsTable, and SubscriptionEntity.
 */
class SubscriptionTest {

    /**
     * Test the Subscription data class.
     */
    @Test
    fun testSubscriptionDataClass() {
        val now = LocalDateTime.now()
        val subscription = Subscription(
            id = 1,
            channelId = "UC123456789",
            topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123456789",
            callbackUrl = "https://example.com/pubsub/youtube",
            leaseSeconds = 3600,
            expiresAt = now.plusSeconds(3600),
            createdAt = now,
            updatedAt = now,
            status = "active"
        )

        // Verify the properties
        Assert.assertEquals(subscription.id, 1, "ID should match")
        Assert.assertEquals(subscription.channelId, "UC123456789", "Channel ID should match")
        Assert.assertEquals(subscription.topic, "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123456789", "Topic should match")
        Assert.assertEquals(subscription.callbackUrl, "https://example.com/pubsub/youtube", "Callback URL should match")
        Assert.assertEquals(subscription.leaseSeconds, 3600, "Lease seconds should match")
        Assert.assertEquals(subscription.expiresAt, now.plusSeconds(3600), "Expires at should match")
        Assert.assertEquals(subscription.createdAt, now, "Created at should match")
        Assert.assertEquals(subscription.updatedAt, now, "Updated at should match")
        Assert.assertEquals(subscription.status, "active", "Status should match")
    }

    /**
     * Test the Subscription data class with default values.
     */
    @Test
    fun testSubscriptionDataClassDefaults() {
        val now = LocalDateTime.now()
        val subscription = Subscription(
            channelId = "UC123456789",
            topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123456789",
            callbackUrl = "https://example.com/pubsub/youtube",
            leaseSeconds = 3600,
            expiresAt = now.plusSeconds(3600)
        )

        // Verify the default properties
        Assert.assertEquals(subscription.id, 0, "Default ID should be 0")
        Assert.assertEquals(subscription.status, "active", "Default status should be 'active'")

        // Verify that createdAt and updatedAt are set to current time (approximately)
        val currentTime = LocalDateTime.now()
        Assert.assertTrue(subscription.createdAt.isBefore(currentTime) || subscription.createdAt.isEqual(currentTime), 
            "Created at should be before or equal to current time")
        Assert.assertTrue(subscription.updatedAt.isBefore(currentTime) || subscription.updatedAt.isEqual(currentTime), 
            "Updated at should be before or equal to current time")
    }

    /**
     * Test the SubscriptionsTable definition.
     */
    @Test
    fun testSubscriptionsTable() {
        // Verify the table name
        Assert.assertEquals(SubscriptionsTable.tableName, "Subscriptions", "Table name should match")

        // Verify the column definitions
        Assert.assertEquals(SubscriptionsTable.channelId.name, "channel_id", "Channel ID column name should match")
        Assert.assertEquals(SubscriptionsTable.topic.name, "topic", "Topic column name should match")
        Assert.assertEquals(SubscriptionsTable.callbackUrl.name, "callback_url", "Callback URL column name should match")
        Assert.assertEquals(SubscriptionsTable.leaseSeconds.name, "lease_seconds", "Lease seconds column name should match")
        Assert.assertEquals(SubscriptionsTable.expiresAt.name, "expires_at", "Expires at column name should match")
        Assert.assertEquals(SubscriptionsTable.createdAt.name, "created_at", "Created at column name should match")
        Assert.assertEquals(SubscriptionsTable.updatedAt.name, "updated_at", "Updated at column name should match")
        Assert.assertEquals(SubscriptionsTable.status.name, "status", "Status column name should match")
    }

    /**
     * Test the SubscriptionEntity conversion to DTO.
     * This test requires a database connection, so we use an in-memory H2 database.
     */
    @Test
    fun testSubscriptionEntityToDTO() {
        // Connect to an in-memory H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        // Create the schema
        transaction {
            SchemaUtils.create(SubscriptionsTable)

            // Create a subscription entity
            val now = LocalDateTime.now()
            val entity = SubscriptionEntity.new {
                channelId = "UC123456789"
                topic = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123456789"
                callbackUrl = "https://example.com/pubsub/youtube"
                leaseSeconds = 3600
                expiresAt = now.plusSeconds(3600)
                createdAt = now
                updatedAt = now
                status = "active"
            }

            // Convert to DTO
            val subscription = entity.toSubscription()

            // Verify the conversion
            Assert.assertEquals(subscription.id, entity.id.value, "ID should match")
            Assert.assertEquals(subscription.channelId, entity.channelId, "Channel ID should match")
            Assert.assertEquals(subscription.topic, entity.topic, "Topic should match")
            Assert.assertEquals(subscription.callbackUrl, entity.callbackUrl, "Callback URL should match")
            Assert.assertEquals(subscription.leaseSeconds, entity.leaseSeconds, "Lease seconds should match")
            Assert.assertEquals(subscription.expiresAt, entity.expiresAt, "Expires at should match")
            Assert.assertEquals(subscription.createdAt, entity.createdAt, "Created at should match")
            Assert.assertEquals(subscription.updatedAt, entity.updatedAt, "Updated at should match")
            Assert.assertEquals(subscription.status, entity.status, "Status should match")
        }
    }
}
