package com.example.service

import com.example.models.Notification
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

class RabbitMQIntegrationTest {
    companion object {
        private val rabbitMQ = RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"))

        @JvmStatic
        @BeforeClass
        fun setUp() {
            try {
                rabbitMQ.start()
            } catch (e: Exception) {
                // Skip tests if Docker is not available
                throw org.testng.SkipException("Docker not available: ${e.message}")
            }

            setEnv("RABBITMQ_HOST", rabbitMQ.host)
            setEnv("RABBITMQ_PORT", rabbitMQ.amqpPort.toString())
            setEnv("RABBITMQ_USERNAME", rabbitMQ.adminUsername)
            setEnv("RABBITMQ_PASSWORD", rabbitMQ.adminPassword)
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            rabbitMQ.stop()
        }

        private fun setEnv(key: String, value: String) {
            val env = System.getenv()
            val cl = env.javaClass
            val field = cl.getDeclaredField("m")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = field.get(env) as MutableMap<String, String>
            map[key] = value
        }
    }

    @Test
    fun testQueueAndConsume() {
        val queueService = NotificationQueueService().apply { init() }
        val consumerService = NotificationConsumerService().apply {
            init()
            startConsuming()
        }

        val notification = Notification(
            videoId = "yt:video:ABC12345678",
            title = "Integration Test",
            channelId = "UC123456789",
            channelName = "Test Channel",
            published = "2023-05-26T12:00:00Z",
            updated = "2023-05-26T12:30:00Z"
        )

        val queued = queueService.queueNotification(notification)
        Assert.assertTrue(queued, "Message should be queued")

        runBlocking { delay(1500) }

        val factory = ConnectionFactory().apply {
            host = rabbitMQ.host
            port = rabbitMQ.amqpPort
            username = rabbitMQ.adminUsername
            password = rabbitMQ.adminPassword
        }

        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                val response = channel.basicGet("youtube_notifications", true)
                Assert.assertNull(response, "Queue should be empty after processing")
            }
        }

        consumerService.stopConsuming()
        queueService.close()
    }
}
