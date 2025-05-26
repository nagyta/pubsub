package com.example.models

import java.time.LocalDateTime

/**
 * Data model for YouTube content notifications.
 * This class represents a notification that is queued for processing.
 */
data class Notification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val videoId: String,
    val title: String,
    val channelId: String?,
    val channelName: String?,
    val published: String?,
    val updated: String?,
    val receivedAt: LocalDateTime = LocalDateTime.now(),
    val status: String = "pending"
)

/**
 * Convert an AtomFeed entry to a Notification.
 */
fun Entry.toNotification(): Notification {
    val channelId = this.author?.uri?.substringAfterLast("/")
    
    return Notification(
        videoId = this.id ?: "",
        title = this.title ?: "",
        channelId = channelId,
        channelName = this.author?.name,
        published = this.published,
        updated = this.updated
    )
}
