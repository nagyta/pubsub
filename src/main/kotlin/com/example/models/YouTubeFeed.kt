package com.example.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

/**
 * Data models for YouTube PubSubHubbub feed notifications.
 * These classes represent the XML structure that YouTube sends when a new video is published.
 *
 * The YouTube PubSubHubbub notification system uses Atom XML format to deliver updates about new content.
 * When a channel publishes a new video, YouTube sends a notification to all subscribers with details about the video.
 *
 * Reference: https://pubsubhubbub.github.io/PubSubHubbub/pubsubhubbub-core-0.4.html
 * YouTube API documentation: https://developers.google.com/youtube/v3/guides/push_notifications
 */

@JacksonXmlRootElement(localName = "feed", namespace = "http://www.w3.org/2005/Atom")
@JsonIgnoreProperties(ignoreUnknown = true)
data class AtomFeed(
    @JacksonXmlProperty(localName = "entry")
    val entry: Entry? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Entry(
    @JacksonXmlProperty(localName = "id")
    val id: String? = null,

    @JacksonXmlProperty(localName = "title")
    val title: String? = null,

    @JacksonXmlProperty(localName = "author")
    val author: Author? = null,

    @JacksonXmlProperty(localName = "published")
    val published: String? = null,

    @JacksonXmlProperty(localName = "updated")
    val updated: String? = null,

    @JacksonXmlProperty(localName = "link")
    val links: List<Link>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author(
    @JacksonXmlProperty(localName = "name")
    val name: String? = null,

    @JacksonXmlProperty(localName = "uri")
    val uri: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Link(
    @JacksonXmlProperty(isAttribute = true, localName = "rel")
    val rel: String? = null,

    @JacksonXmlProperty(isAttribute = true, localName = "href")
    val href: String? = null
)
