package com.example.service

import com.example.models.Notification
import com.example.service.interfaces.INotificationQueueService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.slf4j.LoggerFactory

/**
 * Service for queueing YouTube content notifications using RabbitMQ.
 */
class NotificationQueueService : INotificationQueueService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val queueName = "youtube_notifications"
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Initialize the RabbitMQ connection and channel.
     */
    override fun init() {
        try {
            logger.info("Initializing RabbitMQ connection")

            val factory = ConnectionFactory()
            factory.host = System.getenv("RABBITMQ_HOST") ?: "localhost"
            factory.port = System.getenv("RABBITMQ_PORT")?.toIntOrNull() ?: 5672
            factory.username = System.getenv("RABBITMQ_USERNAME") ?: "guest"
            factory.password = System.getenv("RABBITMQ_PASSWORD") ?: "guest"

            connection = factory.newConnection()
            channel = connection?.createChannel()

            // Declare a durable queue
            channel?.queueDeclare(queueName, true, false, false, null)

            logger.info("RabbitMQ connection initialized")
        } catch (e: IOException) {
            logger.error("Error initializing RabbitMQ connection: ${e.message}", e)
        } catch (e: TimeoutException) {
            logger.error("Timeout initializing RabbitMQ connection: ${e.message}", e)
        }
    }

    /**
     * Queue a notification for processing.
     *
     * @param notification The notification to queue
     * @return True if the notification was queued successfully, false otherwise
     */
    override fun queueNotification(notification: Notification): Boolean {
        try {
            if (channel == null || !channel!!.isOpen) {
                logger.warn("Channel is not open, attempting to reconnect")
                init()
                if (channel == null || !channel!!.isOpen) {
                    logger.error("Failed to reconnect to RabbitMQ")
                    return false
                }
            }

            // Convert notification to JSON
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Publish the notification to the queue
            channel?.basicPublish("", queueName, null, notificationJson.toByteArray())

            logger.info("Notification queued: ${notification.title} (ID: ${notification.videoId})")
            return true
        } catch (e: Exception) {
            logger.error("Error queueing notification: ${e.message}", e)
            return false
        }
    }

    /**
     * Close the RabbitMQ connection and channel.
     */
    override fun close() {
        try {
            channel?.close()
            connection?.close()
            logger.info("RabbitMQ connection closed")
        } catch (e: IOException) {
            logger.error("Error closing RabbitMQ connection: ${e.message}", e)
        } catch (e: TimeoutException) {
            logger.error("Timeout closing RabbitMQ connection: ${e.message}", e)
        }
    }

    /**
     * Check if the RabbitMQ connection is available.
     *
     * @return True if the connection is available, false otherwise
     */
    override fun isAvailable(): Boolean {
        return try {
            if (channel == null || !channel!!.isOpen) {
                logger.warn("Channel is not open, attempting to reconnect")
                init()
            }

            channel != null && channel!!.isOpen
        } catch (e: Exception) {
            logger.error("Error checking RabbitMQ availability: ${e.message}", e)
            false
        }
    }
}
