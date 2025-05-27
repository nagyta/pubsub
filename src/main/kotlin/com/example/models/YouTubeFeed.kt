package com.example.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data models for YouTube PubSubHubbub feed notifications.
 * These classes represent the JSON structure for handling video notifications.
 *
 * The YouTube PubSubHubbub notification system uses Atom format to deliver updates about new content.
 * When a channel publishes a new video, YouTube sends a notification to all subscribers with details about the video.
 *
 * Reference: https://pubsubhubbub.github.io/PubSubHubbub/pubsubhubbub-core-0.4.html
 * YouTube API documentation: https://developers.google.com/youtube/v3/guides/push_notifications
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class AtomFeed(
    @JsonProperty("entry")
    val entry: Entry? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Entry(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("author")
    val author: Author? = null,

    @JsonProperty("published")
    val published: String? = null,

    @JsonProperty("updated")
    val updated: String? = null,

    @JsonProperty("link")
    val links: List<Link>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("uri")
    val uri: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Link(
    @JsonProperty("rel")
    val rel: String? = null,

    @JsonProperty("href")
    val href: String? = null
)
