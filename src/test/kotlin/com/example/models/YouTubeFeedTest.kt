package com.example.models

import org.testng.annotations.Test
import org.testng.Assert
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule

/**
 * TestNG tests for the YouTube feed models.
 * Tests the parsing of YouTube's Atom XML feed format.
 */
class YouTubeFeedTest {

    private val xmlMapper: XmlMapper = createXmlMapper()

    /**
     * Creates an XmlMapper with Kotlin support for testing.
     * This is similar to the one used in the application.
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
    fun testParseAtomFeed() {
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

        // Parse the XML into our data classes
        val feed = xmlMapper.readValue(sampleXml, AtomFeed::class.java)

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
        val emptyXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
            </feed>
        """.trimIndent()

        val feed = xmlMapper.readValue(emptyXml, AtomFeed::class.java)
        
        Assert.assertNotNull(feed, "Feed should not be null even when empty")
        Assert.assertNull(feed.entry, "Entry should be null for empty feed")
    }

    @Test
    fun testParsePartialFeed() {
        // Test with a partial feed (missing some fields)
        val partialXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <entry>
                    <id>yt:video:XYZ98765432</id>
                    <title>Partial Test Video</title>
                    <!-- Missing author -->
                    <!-- Missing published date -->
                    <updated>2023-05-26T14:00:00Z</updated>
                    <!-- Missing link -->
                </entry>
            </feed>
        """.trimIndent()

        val feed = xmlMapper.readValue(partialXml, AtomFeed::class.java)
        
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
