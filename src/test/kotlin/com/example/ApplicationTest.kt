package com.example

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
 * TestNG tests for the application configuration.
 * Tests that the application is properly configured with the necessary plugins.
 */
class ApplicationTest {

    @Test
    fun testApplicationConfiguration() = testApplication {
        application {
            module()
        }
        // Test that the application is properly configured
        // by making a request that exercises the content negotiation plugin

        // Sample JSON that mimics YouTube's Atom feed format
        val sampleJson = """
            {
                "entry": {
                    "id": "yt:video:ABC12345678",
                    "title": "Test Video Title",
                    "author": {
                        "name": "Test Channel"
                    }
                }
            }
        """.trimIndent()

        // Make a request that uses JSON content negotiation
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(sampleJson)
        }

        // If the application is properly configured, this should return OK
        Assert.assertEquals(response.status, HttpStatusCode.OK)
    }

    @Test
    fun testErrorHandling() = testApplication {
        application {
            module()
        }
        // Test that the status pages plugin is properly configured
        // by making a request that should trigger an error

        // Invalid JSON that should cause an error
        val invalidJson = "This is not JSON"

        // Make a request with invalid JSON
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Json)
            setBody(invalidJson)
        }

        // If the status pages plugin is properly configured, this should return BadRequest
        // and include an error message
        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Invalid request format"))
    }

    @Test
    fun testContentNegotiation() = testApplication {
        application {
            module()
        }
        // Test that the content negotiation plugin is properly configured
        // by making a request with an unsupported content type

        // Make a request with an unsupported content type
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Text.Plain)
            setBody("This is plain text, not JSON")
        }

        // The application tries to parse the plain text as JSON and fails with a BadRequest
        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
    }
}
