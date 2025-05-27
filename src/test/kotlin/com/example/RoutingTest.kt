package com.example

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.testng.Assert
import org.testng.annotations.Test

/**
 * TestNG tests for the routing functionality.
 * Tests the endpoints for subscription verification and content notifications.
 */
class RoutingTest {

    @Test
    fun testHomePage() = testApplication {
        application {
            module()
        }
        // Test the home page endpoint
        val response = client.get("/")
        Assert.assertEquals(response.status, HttpStatusCode.OK)
        Assert.assertEquals(response.bodyAsText(), "YouTube PubSubHubbub Service")
    }

    @Test
    fun testSubscriptionVerification() = testApplication {
        application {
            module()
        }
        // Test subscription verification with hub.challenge
        val challenge = "test_challenge_123"
        val response = client.get("/pubsub/youtube") {
            parameter("hub.challenge", challenge)
        }

        Assert.assertEquals(response.status, HttpStatusCode.OK)
        Assert.assertEquals(response.bodyAsText(), challenge)
    }

    @Test
    fun testSubscriptionVerificationMissingChallenge() = testApplication {
        application {
            module()
        }
        // Test subscription verification without hub.challenge
        val response = client.get("/pubsub/youtube")

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing hub.challenge parameter"))
    }

    @Test
    fun testContentNotification() = testApplication {
        application {
            module()
        }
        // Sample JSON that mimics YouTube's Atom feed format
        val sampleJson = """
            {
                "entry": {
                    "id": "yt:video:ABC12345678",
                    "title": "Test Video Title",
                    "author": {
                        "name": "Test Channel",
                        "uri": "https://www.youtube.com/channel/UC123456789"
                    },
                    "published": "2023-05-26T12:00:00Z",
                    "updated": "2023-05-26T12:30:00Z",
                    "link": [
                        {
                            "rel": "alternate",
                            "href": "https://www.youtube.com/watch?v=ABC12345678"
                        }
                    ]
                }
            }
        """.trimIndent()

        // Test content notification with valid JSON
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.OK)
    }

    @Test
    fun testContentNotificationMissingTitle() = testApplication {
        application {
            module()
        }
        // Sample JSON with missing title
        val sampleJson = """
            {
                "entry": {
                    "id": "yt:video:ABC12345678",
                    "author": {
                        "name": "Test Channel",
                        "uri": "https://www.youtube.com/channel/UC123456789"
                    }
                }
            }
        """.trimIndent()

        // Test content notification with missing title
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing or empty title in feed"))
    }

    @Test
    fun testContentNotificationInvalidJson() = testApplication {
        application {
            module()
        }
        // Invalid JSON
        val invalidJson = """
            {
                "invalid": "This is not a valid Atom feed"
            }
        """.trimIndent()

        // Test content notification with invalid JSON
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(invalidJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing entry element in feed"))
    }

    @Test
    fun testContentNotificationMissingVideoId() = testApplication {
        application {
            module()
        }
        // Sample JSON with missing video ID
        val sampleJson = """
            {
                "entry": {
                    "title": "Test Video Title",
                    "author": {
                        "name": "Test Channel",
                        "uri": "https://www.youtube.com/channel/UC123456789"
                    }
                }
            }
        """.trimIndent()

        // Test content notification with missing video ID
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing or empty video ID in feed"))
    }

    @Test
    fun testContentNotificationEmptyTitle() = testApplication {
        application {
            module()
        }
        // Sample JSON with empty title
        val sampleJson = """
            {
                "entry": {
                    "id": "yt:video:ABC12345678",
                    "title": "",
                    "author": {
                        "name": "Test Channel",
                        "uri": "https://www.youtube.com/channel/UC123456789"
                    }
                }
            }
        """.trimIndent()

        // Test content notification with empty title
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing or empty title in feed"))
    }

    @Test
    fun testContentNotificationMissingEntry() = testApplication {
        application {
            module()
        }
        // Sample JSON with missing entry
        val sampleJson = """
            {
                "feed": "Empty feed without entry"
            }
        """.trimIndent()

        // Test content notification with missing entry
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing entry element in feed"))
    }
}
