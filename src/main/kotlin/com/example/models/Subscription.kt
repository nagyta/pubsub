package com.example.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Data models for YouTube PubSubHubbub subscriptions.
 * These classes represent the subscription data that is stored in the database.
 */

/**
 * Database table definition for subscriptions
 */
object SubscriptionsTable : IntIdTable() {
    val channelId = varchar("channel_id", 50).uniqueIndex()
    val topic = varchar("topic", 255)
    val callbackUrl = varchar("callback_url", 255)
    val leaseSeconds = integer("lease_seconds")
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val status = varchar("status", 20).default("active")
}

/**
 * DAO for Subscription entity
 */
class SubscriptionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : org.jetbrains.exposed.dao.IntEntityClass<SubscriptionEntity>(SubscriptionsTable)

    var channelId by SubscriptionsTable.channelId
    var topic by SubscriptionsTable.topic
    var callbackUrl by SubscriptionsTable.callbackUrl
    var leaseSeconds by SubscriptionsTable.leaseSeconds
    var expiresAt by SubscriptionsTable.expiresAt
    var createdAt by SubscriptionsTable.createdAt
    var updatedAt by SubscriptionsTable.updatedAt
    var status by SubscriptionsTable.status

    /**
     * Convert DAO to DTO
     */
    fun toSubscription() = Subscription(
        id = id.value,
        channelId = channelId,
        topic = topic,
        callbackUrl = callbackUrl,
        leaseSeconds = leaseSeconds,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = status
    )
}

/**
 * Data Transfer Object for Subscription
 */
data class Subscription(
    val id: Int = 0,
    val channelId: String,
    val topic: String,
    val callbackUrl: String,
    val leaseSeconds: Int,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val status: String = "active"
)
