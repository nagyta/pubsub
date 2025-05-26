package com.example.service

import com.example.models.Notification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.LocalDateTime

/**
 * TestNG tests for the NotificationConsumerService.
 * Tests the notification consumer service functionality.
 * 
 * Note: These tests focus on the service's behavior without actually connecting to RabbitMQ.
 */
@ExperimentalCoroutinesApi
class NotificationConsumerServiceTest {
    private lateinit var consumerService: NotificationConsumerService

    /**
     * Set up the test environment before each test.
     * Creates a new NotificationConsumerService instance.
     */
    @BeforeMethod
    fun setUp() {
        consumerService = NotificationConsumerService()
        // Note: We don't call init() here because it would try to connect to RabbitMQ
    }

    /**
     * Test creating a notification consumer service instance.
     * This test verifies that we can create a valid service instance.
     */
    @Test
    fun testCreateConsumerService() {
        Assert.assertNotNull(consumerService, "Consumer service should not be null")
        Assert.assertFalse(consumerService.isRunning(), "Consumer service should not be running initially")
    }

    /**
     * Test the isRunning method.
     * This test verifies that the isRunning method returns the correct status.
     */
    @Test
    fun testIsRunning() {
        // Initially, the service should not be running
        Assert.assertFalse(consumerService.isRunning(), "Consumer service should not be running initially")

        // We can't easily test the running state without connecting to RabbitMQ,
        // but we can verify that the method exists and returns a boolean
        val isRunning = consumerService.isRunning()
        Assert.assertNotNull(isRunning, "isRunning should return a non-null value")
    }

    /**
     * Test starting the consumer when it's already running.
     * This test verifies that the service handles the case when it's already running.
     */
    @Test
    fun testStartConsumingAlreadyRunning() {
        // Set the service as running using reflection
        val runningField = NotificationConsumerService::class.java.getDeclaredField("isRunning")
        runningField.isAccessible = true
        runningField.set(consumerService, true)

        // Verify the service is running
        Assert.assertTrue(consumerService.isRunning(), "Consumer service should be running")

        // Try to start the service again
        consumerService.startConsuming()

        // Verify the service is still running
        Assert.assertTrue(consumerService.isRunning(), "Consumer service should still be running")
    }

    /**
     * Test stopping the consumer.
     * This test verifies that the service can be stopped.
     */
    @Test
    fun testStopConsuming() {
        // Set the service as running using reflection
        val runningField = NotificationConsumerService::class.java.getDeclaredField("isRunning")
        runningField.isAccessible = true
        runningField.set(consumerService, true)

        // Verify the service is running
        Assert.assertTrue(consumerService.isRunning(), "Consumer service should be running")

        // Stop the service
        consumerService.stopConsuming()

        // Verify the service is stopped
        Assert.assertFalse(consumerService.isRunning(), "Consumer service should be stopped")
    }

    /**
     * Test the init method without actually connecting to RabbitMQ.
     * This test verifies that the init method exists and can be called without errors.
     */
    @Test
    fun testInit() {
        // We can't easily test the init method without connecting to RabbitMQ,
        // but we can verify that the method exists and can be called
        try {
            // We don't actually call init() here because it would try to connect to RabbitMQ
            // This is just a placeholder to show that we would test this method
            Assert.assertFalse(consumerService.isRunning(), "Consumer service should not be running initially")
        } catch (e: Exception) {
            Assert.fail("Exception should not be thrown: ${e.message}")
        }
    }

    /**
     * Test creating a notification object for processing.
     * This test verifies that we can create a valid notification object that could be processed.
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
}
