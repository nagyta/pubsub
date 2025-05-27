package com.example.service.interfaces

import com.example.models.Notification

/**
 * Interface for notification consumer operations.
 * This interface defines the contract for notification consumer services.
 */
interface INotificationConsumerService {
    /**
     * Initialize the notification consumer service.
     */
    fun init()

    /**
     * Start consuming notifications from the queue.
     */
    fun startConsuming()

    /**
     * Process a notification.
     *
     * @param notification The notification to process
     */
    suspend fun processNotification(notification: Notification)

    /**
     * Check if the consumer is running.
     *
     * @return True if the consumer is running, false otherwise
     */
    fun isRunning(): Boolean

    /**
     * Stop consuming notifications.
     */
    fun stopConsuming()
}
