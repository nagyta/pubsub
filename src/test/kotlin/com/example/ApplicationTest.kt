package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
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

        // Sample XML that mimics YouTube's Atom feed format
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <entry>
                    <id>yt:video:ABC12345678</id>
                    <title>Test Video Title</title>
                    <author>
                        <name>Test Channel</name>
                    </author>
                </entry>
            </feed>
        """.trimIndent()

        // Make a request that uses XML content negotiation
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Xml)
            setBody(sampleXml)
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

        // Invalid XML that should cause an error
        val invalidXml = "This is not XML"

        // Make a request with invalid XML
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Xml)
            setBody(invalidXml)
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
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // The application tries to parse the JSON as XML and fails with a BadRequest
        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
    }
}
