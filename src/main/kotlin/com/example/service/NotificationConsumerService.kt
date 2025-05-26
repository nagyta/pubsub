package com.example.service

import com.example.models.Notification
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext

/**
 * Service for consuming and processing YouTube content notifications from RabbitMQ.
 * This service is part of Phase 3 implementation, adding the ability to process
 * notifications that were previously only queued.
 */
class NotificationConsumerService : CoroutineScope {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val queueName = "youtube_notifications"
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    // Coroutine context and job for managing coroutines
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var isRunning = false

    /**
     * Initialize the RabbitMQ connection and channel.
     */
    fun init() {
        try {
            logger.info("Initializing NotificationConsumerService")

            val factory = ConnectionFactory()
            factory.host = "localhost"
            factory.port = 5672
            factory.username = "guest"
            factory.password = "guest"

            connection = factory.newConnection()
            channel = connection?.createChannel()

            // Declare the queue (same as in NotificationQueueService)
            channel?.queueDeclare(queueName, true, false, false, null)

            logger.info("NotificationConsumerService initialized")
        } catch (e: IOException) {
            logger.error("Error initializing RabbitMQ connection: ${e.message}", e)
        } catch (e: TimeoutException) {
            logger.error("Timeout initializing RabbitMQ connection: ${e.message}", e)
        }
    }

    /**
     * Start consuming notifications from the queue.
     */
    fun startConsuming() {
        if (isRunning) {
            logger.warn("Consumer is already running")
            return
        }

        launch {
            try {
                if (channel == null || !channel!!.isOpen) {
                    logger.warn("Channel is not open, attempting to reconnect")
                    init()
                    if (channel == null || !channel!!.isOpen) {
                        logger.error("Failed to reconnect to RabbitMQ")
                        return@launch
                    }
                }

                logger.info("Starting to consume notifications from queue: $queueName")
                isRunning = true

                // Set prefetch count to limit the number of unacknowledged messages
                channel?.basicQos(1)

                // Create a consumer that processes messages from the queue
                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray
                    ) {
                        // Launch a coroutine for each message
                        launch {
                            try {
                                val message = String(body)
                                val notification = objectMapper.readValue<Notification>(message)

                                logger.info("Processing notification: ${notification.title} (ID: ${notification.videoId})")

                                // Process the notification
                                processNotification(notification)

                                // Acknowledge the message
                                channel?.basicAck(envelope.deliveryTag, false)

                                logger.info("Notification processed successfully: ${notification.title}")
                            } catch (e: Exception) {
                                logger.error("Error processing notification: ${e.message}", e)

                                // Reject the message and requeue it
                                channel?.basicNack(envelope.deliveryTag, false, true)
                            }
                        }
                    }
                }

                // Start consuming messages
                channel?.basicConsume(queueName, false, consumer)

                logger.info("Now consuming notifications from queue: $queueName")
            } catch (e: Exception) {
                isRunning = false
                logger.error("Error starting consumer: ${e.message}", e)
            }
        }
    }

    /**
     * Process a notification.
     * This method can be extended to perform additional actions based on the notification.
     *
     * @param notification The notification to process
     */
    private suspend fun processNotification(notification: Notification) {
        // Log detailed information about the notification
        logger.info("Processing YouTube notification:")
        logger.info("- Title: ${notification.title}")
        logger.info("- Video ID: ${notification.videoId}")
        logger.info("- Channel: ${notification.channelName} (ID: ${notification.channelId})")
        logger.info("- Published: ${notification.published}")
        logger.info("- Received: ${notification.receivedAt}")

        // Here you can add additional processing logic:
        // - Update a database record
        // - Send notifications to other systems
        // - Trigger workflows based on the content
        // - etc.

        // Simulate some processing time using coroutine delay instead of Thread.sleep
        delay(100)
    }

    /**
     * Check if the consumer is running.
     *
     * @return True if the consumer is running, false otherwise
     */
    fun isRunning(): Boolean {
        return isRunning
    }

    /**
     * Stop consuming notifications.
     */
    fun stopConsuming() {
        try {
            logger.info("Stopping notification consumer")
            isRunning = false

            // Close channel and connection
            channel?.close()
            connection?.close()

            // Cancel all coroutines
            job.cancel()

            // Wait for all coroutines to complete (with timeout)
            runBlocking {
                withTimeout(5000) {
                    job.join()
                }
            }

            logger.info("Notification consumer stopped")
        } catch (e: IOException) {
            logger.error("Error stopping consumer: ${e.message}", e)
        } catch (e: TimeoutException) {
            logger.error("Timeout stopping consumer: ${e.message}", e)
        } catch (e: CancellationException) {
            logger.info("Coroutines cancelled successfully")
        } catch (e: Exception) {
            logger.error("Error while stopping consumer: ${e.message}", e)
        }
    }
}
