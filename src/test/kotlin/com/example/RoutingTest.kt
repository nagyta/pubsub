package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.testng.Assert
import org.testng.annotations.Test
import com.example.models.AtomFeed
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule

/**
 * TestNG tests for the routing functionality.
 * Tests the endpoints for subscription verification and content notifications.
 */
class RoutingTest {

    /**
     * Creates an XmlMapper with Kotlin support for testing.
     */
    private fun createXmlMapper(): XmlMapper {
        val xmlModule = JacksonXmlModule().apply {
            setDefaultUseWrapper(false)
        }
        return XmlMapper(xmlModule).apply {
            registerKotlinModule()
        }
    }

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
        // Sample XML that mimics YouTube's Atom feed format
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <entry>
                    <id>yt:video:ABC12345678</id>
                    <title>Test Video Title</title>
                    <author>
                        <name>Test Channel</name>
                        <uri>https://www.youtube.com/channel/UC123456789</uri>
                    </author>
                    <published>2023-05-26T12:00:00Z</published>
                    <updated>2023-05-26T12:30:00Z</updated>
                    <link rel="alternate" href="https://www.youtube.com/watch?v=ABC12345678"/>
                </entry>
            </feed>
        """.trimIndent()

        // Test content notification with valid XML
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Xml)
            setBody(sampleXml)
        }

        Assert.assertEquals(response.status, HttpStatusCode.OK)
    }

    @Test
    fun testContentNotificationMissingTitle() = testApplication {
        application {
            module()
        }
        // Sample XML with missing title
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <entry>
                    <id>yt:video:ABC12345678</id>
                    <!-- Missing title -->
                    <author>
                        <name>Test Channel</name>
                        <uri>https://www.youtube.com/channel/UC123456789</uri>
                    </author>
                </entry>
            </feed>
        """.trimIndent()

        // Test content notification with missing title
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Xml)
            setBody(sampleXml)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing title in feed"))
    }

    @Test
    fun testContentNotificationInvalidXml() = testApplication {
        application {
            module()
        }
        // Invalid XML
        val invalidXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <invalid>
                This is not a valid Atom feed
            </invalid>
        """.trimIndent()

        // Test content notification with invalid XML
        val response = client.post("/pubsub/youtube") {
            contentType(ContentType.Application.Xml)
            setBody(invalidXml)
        }

        Assert.assertEquals(response.status, HttpStatusCode.BadRequest)
        Assert.assertTrue(response.bodyAsText().contains("Missing title in feed"))
    }
}
