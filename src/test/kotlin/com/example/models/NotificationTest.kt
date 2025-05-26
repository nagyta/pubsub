package com.example.models

import org.testng.annotations.Test
import org.testng.Assert
import java.time.LocalDateTime

/**
 * TestNG tests for the Notification model.
 * Tests the Notification data class and the toNotification extension function.
 */
class NotificationTest {

    /**
     * Test the Notification data class.
     */
    @Test
    fun testNotificationDataClass() {
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

        // Verify the properties
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
     * Test the Notification data class with default values.
     */
    @Test
    fun testNotificationDataClassDefaults() {
        val notification = Notification(
            videoId = "yt:video:ABC12345678",
            title = "Test Video Title",
            channelId = null,
            channelName = null,
            published = null,
            updated = null
        )

        // Verify the default properties
        Assert.assertNotNull(notification.id, "ID should not be null")
        Assert.assertTrue(notification.id.isNotEmpty(), "ID should not be empty")
        Assert.assertEquals(notification.status, "pending", "Default status should be 'pending'")
        
        // Verify that receivedAt is set to current time (approximately)
        val currentTime = LocalDateTime.now()
        Assert.assertTrue(notification.receivedAt.isBefore(currentTime) || notification.receivedAt.isEqual(currentTime), 
            "Received at should be before or equal to current time")
    }

    /**
     * Test the toNotification extension function on Entry.
     */
    @Test
    fun testEntryToNotification() {
        // Create an Entry object
        val entry = Entry(
            id = "yt:video:ABC12345678",
            title = "Test Video Title",
            author = Author(
                name = "Test Channel",
                uri = "https://www.youtube.com/channel/UC123456789"
            ),
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z",
            links = listOf(
                Link(
                    rel = "alternate",
                    href = "https://www.youtube.com/watch?v=ABC12345678"
                )
            )
        )

        // Convert to Notification
        val notification = entry.toNotification()

        // Verify the conversion
        Assert.assertNotNull(notification.id, "ID should not be null")
        Assert.assertEquals(notification.videoId, "yt:video:ABC12345678", "Video ID should match")
        Assert.assertEquals(notification.title, "Test Video Title", "Title should match")
        Assert.assertEquals(notification.channelId, "UC123456789", "Channel ID should match")
        Assert.assertEquals(notification.channelName, "Test Channel", "Channel name should match")
        Assert.assertEquals(notification.published, "2023-05-26T12:00:00Z", "Published date should match")
        Assert.assertEquals(notification.updated, "2023-05-26T12:30:00Z", "Updated date should match")
        Assert.assertEquals(notification.status, "pending", "Status should be 'pending'")
    }

    /**
     * Test the toNotification extension function with partial Entry data.
     */
    @Test
    fun testEntryToNotificationPartial() {
        // Create an Entry object with minimal data
        val entry = Entry(
            id = "yt:video:ABC12345678",
            title = "Test Video Title"
            // Missing author, published, updated, links
        )

        // Convert to Notification
        val notification = entry.toNotification()

        // Verify the conversion
        Assert.assertNotNull(notification.id, "ID should not be null")
        Assert.assertEquals(notification.videoId, "yt:video:ABC12345678", "Video ID should match")
        Assert.assertEquals(notification.title, "Test Video Title", "Title should match")
        Assert.assertNull(notification.channelId, "Channel ID should be null")
        Assert.assertNull(notification.channelName, "Channel name should be null")
        Assert.assertNull(notification.published, "Published date should be null")
        Assert.assertNull(notification.updated, "Updated date should be null")
        Assert.assertEquals(notification.status, "pending", "Status should be 'pending'")
    }

    /**
     * Test the toNotification extension function with null Entry data.
     */
    @Test
    fun testEntryToNotificationNulls() {
        // Create an Entry object with null values
        val entry = Entry(
            id = null,
            title = null,
            author = null,
            published = null,
            updated = null,
            links = null
        )

        // Convert to Notification
        val notification = entry.toNotification()

        // Verify the conversion
        Assert.assertNotNull(notification.id, "ID should not be null")
        Assert.assertEquals(notification.videoId, "", "Video ID should be empty string")
        Assert.assertEquals(notification.title, "", "Title should be empty string")
        Assert.assertNull(notification.channelId, "Channel ID should be null")
        Assert.assertNull(notification.channelName, "Channel name should be null")
        Assert.assertNull(notification.published, "Published date should be null")
        Assert.assertNull(notification.updated, "Updated date should be null")
        Assert.assertEquals(notification.status, "pending", "Status should be 'pending'")
    }
}
