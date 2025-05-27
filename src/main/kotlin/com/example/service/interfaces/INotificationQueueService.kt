package com.example.service.interfaces

import com.example.models.Notification

/**
 * Interface for notification queue operations.
 * This interface defines the contract for notification queue services.
 */
interface INotificationQueueService {
    /**
     * Initialize the notification queue service.
     */
    fun init()

    /**
     * Queue a notification for processing.
     *
     * @param notification The notification to queue
     * @return True if the notification was queued successfully, false otherwise
     */
    fun queueNotification(notification: Notification): Boolean

    /**
     * Close the notification queue service.
     */
    fun close()

    /**
     * Check if the notification queue service is available.
     *
     * @return True if the service is available, false otherwise
     */
    fun isAvailable(): Boolean
}
