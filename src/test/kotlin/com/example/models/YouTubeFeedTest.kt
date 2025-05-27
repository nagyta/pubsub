package com.example.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.testng.Assert
import org.testng.annotations.Test

/**
 * TestNG tests for the YouTube feed models.
 * Tests the parsing of YouTube's Atom feed format using JSON.
 */
class YouTubeFeedTest {

    private val jsonMapper: ObjectMapper = createJsonMapper()

    /**
     * Creates a JSON ObjectMapper with Kotlin support for testing.
     * This is similar to the one used in the application.
     */
    private fun createJsonMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            registerKotlinModule()
        }
    }

    @Test
    fun testParseAtomFeed() {
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

        // Parse the JSON into our data classes
        val feed = jsonMapper.readValue<AtomFeed>(sampleJson)

        // Verify the parsed data
        Assert.assertNotNull(feed, "Feed should not be null")
        Assert.assertNotNull(feed.entry, "Entry should not be null")

        val entry = feed.entry!!
        Assert.assertEquals(entry.id, "yt:video:ABC12345678", "Video ID should match")
        Assert.assertEquals(entry.title, "Test Video Title", "Title should match")

        Assert.assertNotNull(entry.author, "Author should not be null")
        Assert.assertEquals(entry.author?.name, "Test Channel", "Channel name should match")
        Assert.assertEquals(entry.author?.uri, "https://www.youtube.com/channel/UC123456789", "Channel URI should match")

        Assert.assertEquals(entry.published, "2023-05-26T12:00:00Z", "Published date should match")
        Assert.assertEquals(entry.updated, "2023-05-26T12:30:00Z", "Updated date should match")

        Assert.assertNotNull(entry.links, "Links should not be null")
        Assert.assertEquals(entry.links?.size, 1, "Should have one link")
        Assert.assertEquals(entry.links?.get(0)?.rel, "alternate", "Link rel should be 'alternate'")
        Assert.assertEquals(entry.links?.get(0)?.href, "https://www.youtube.com/watch?v=ABC12345678", "Link href should match")
    }

    @Test
    fun testParseEmptyFeed() {
        // Test with an empty feed
        val emptyJson = """
            {
            }
        """.trimIndent()

        val feed = jsonMapper.readValue<AtomFeed>(emptyJson)

        Assert.assertNotNull(feed, "Feed should not be null even when empty")
        Assert.assertNull(feed.entry, "Entry should be null for empty feed")
    }

    @Test
    fun testParsePartialFeed() {
        // Test with a partial feed (missing some fields)
        val partialJson = """
            {
                "entry": {
                    "id": "yt:video:XYZ98765432",
                    "title": "Partial Test Video",
                    "updated": "2023-05-26T14:00:00Z"
                }
            }
        """.trimIndent()

        val feed = jsonMapper.readValue<AtomFeed>(partialJson)

        Assert.assertNotNull(feed, "Feed should not be null")
        Assert.assertNotNull(feed.entry, "Entry should not be null")

        val entry = feed.entry!!
        Assert.assertEquals(entry.id, "yt:video:XYZ98765432", "Video ID should match")
        Assert.assertEquals(entry.title, "Partial Test Video", "Title should match")

        Assert.assertNull(entry.author, "Author should be null")
        Assert.assertNull(entry.published, "Published date should be null")
        Assert.assertEquals(entry.updated, "2023-05-26T14:00:00Z", "Updated date should match")
        Assert.assertNull(entry.links, "Links should be null")
    }
}
