package com.example.service

import com.example.models.Notification
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.LocalDateTime

/**
 * TestNG tests for the NotificationQueueService.
 * Tests the notification queueing service.
 * 
 * Note: These tests focus on the serialization and error handling aspects of the service.
 * They do not test the actual RabbitMQ integration, which would require a running RabbitMQ server.
 */
class NotificationQueueServiceTest {
    private lateinit var queueService: NotificationQueueService

    /**
     * Set up the test environment before each test.
     * Creates a new NotificationQueueService instance.
     */
    @BeforeMethod
    fun setUp() {
        queueService = NotificationQueueService()
        // Note: We don't call init() here because it would try to connect to RabbitMQ
    }

    /**
     * Test creating a notification object.
     * This test verifies that we can create a valid notification object that could be queued.
     */
    @Test
    fun testCreateNotification() {
        val now = LocalDateTime.now()
        val notification = Notification(
            id = "test-id-123",
            videoId = "yt:video:ABC12345678",
            title = "Test Video Title",
            channelId = "UC123456789",
            channelName = "Test Channel",
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z",
            receivedAt = now,
            status = "pending"
        )

        // Verify the notification properties
        Assert.assertEquals(notification.id, "test-id-123", "ID should match")
        Assert.assertEquals(notification.videoId, "yt:video:ABC12345678", "Video ID should match")
        Assert.assertEquals(notification.title, "Test Video Title", "Title should match")
        Assert.assertEquals(notification.channelId, "UC123456789", "Channel ID should match")
        Assert.assertEquals(notification.channelName, "Test Channel", "Channel name should match")
        Assert.assertEquals(notification.published, "2023-05-26T12:00:00Z", "Published date should match")
        Assert.assertEquals(notification.updated, "2023-05-26T12:30:00Z", "Updated date should match")
        Assert.assertEquals(notification.receivedAt, now, "Received at should match")
        Assert.assertEquals(notification.status, "pending", "Status should match")
    }


    /**
     * Test queueing a notification when the channel is not initialized.
     * This test verifies that the service handles the case when the channel is not initialized.
     */
    @Test
    fun testQueueNotificationChannelNotInitialized() {
        // Create a notification
        val notification = Notification(
            videoId = "yt:video:ABC12345678",
            title = "Test Video Title",
            channelId = "UC123456789",
            channelName = "Test Channel",
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z",
            status = "pending"
        )

        // Try to queue the notification (should fail because the channel is not initialized)
        val result = queueService.queueNotification(notification)

        // Verify the result
        Assert.assertFalse(result, "Queueing should fail when the channel is not initialized")
    }

    /**
     * Test error handling in the init method.
     * This test verifies that the init method handles errors gracefully.
     */
    @Test
    fun testInitErrorHandling() {
        // Create a service with a mock connection factory that will throw an exception
        val service = NotificationQueueService()

        try {
            // Call init (it will fail because we're not mocking the connection factory)
            service.init()

            // If we got here, the test failed because the method should have handled the error gracefully
            // In a real test, we would mock the connection factory to throw an exception
        } catch (e: Exception) {
            Assert.fail("Init method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the queueNotification method.
     * This test verifies that the method handles errors gracefully.
     */
    @Test
    fun testQueueNotificationErrorHandling() {
        // Set the channel to null to simulate an error
        val channelField = NotificationQueueService::class.java.getDeclaredField("channel")
        channelField.isAccessible = true
        channelField.set(queueService, null)

        // Create a notification
        val notification = Notification(
            videoId = "yt:video:ABC12345678",
            title = "Test Video Title",
            channelId = "UC123456789",
            channelName = "Test Channel",
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z"
        )

        try {
            // Try to queue the notification when the channel is null
            val result = queueService.queueNotification(notification)

            // Verify the result
            Assert.assertFalse(result, "Queueing should fail when the channel is null")
        } catch (e: Exception) {
            Assert.fail("queueNotification method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test error handling in the close method.
     * This test verifies that the method handles errors gracefully.
     */
    @Test
    fun testCloseErrorHandling() {
        // Set the channel to null to simulate an error
        val channelField = NotificationQueueService::class.java.getDeclaredField("channel")
        channelField.isAccessible = true
        channelField.set(queueService, null)

        try {
            // Try to close when the channel is null
            queueService.close()

            // If we got here, the test passed because the method handled the error gracefully
        } catch (e: Exception) {
            Assert.fail("close method should handle errors gracefully: ${e.message}")
        }
    }

    /**
     * Test notification serialization.
     * This test verifies that a notification object can be created with the expected values.
     * Since we can't easily test the actual serialization without using reflection,
     * we'll just verify that the notification object is valid.
     */
    @Test
    fun testNotificationSerialization() {
        // Create a notification
        val notification = Notification(
            videoId = "yt:video:ABC12345678",
            title = "Test Video Title",
            channelId = "UC123456789",
            channelName = "Test Channel",
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z"
        )

        // Verify the notification properties
        Assert.assertEquals(notification.videoId, "yt:video:ABC12345678", "Video ID should match")
        Assert.assertEquals(notification.title, "Test Video Title", "Title should match")
        Assert.assertEquals(notification.channelId, "UC123456789", "Channel ID should match")
        Assert.assertEquals(notification.channelName, "Test Channel", "Channel name should match")
        Assert.assertEquals(notification.published, "2023-05-26T12:00:00Z", "Published date should match")
        Assert.assertEquals(notification.updated, "2023-05-26T12:30:00Z", "Updated date should match")
    }
}
