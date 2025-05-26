package com.example.repository

import com.example.cacheService
import com.example.models.Subscription
import com.example.models.SubscriptionEntity
import com.example.models.SubscriptionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Repository for managing YouTube PubSubHubbub subscriptions in the database.
 */
class SubscriptionRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Initialize the database and create the necessary tables.
     */
    fun init() {
        logger.info("Initializing subscription database")
        transaction {
            SchemaUtils.create(SubscriptionsTable)
        }
        logger.info("Subscription database initialized")
    }

    /**
     * Create a new subscription or update an existing one.
     * Updates the cache when a subscription is created or updated.
     *
     * @param channelId The YouTube channel ID
     * @param topic The topic URL
     * @param callbackUrl The callback URL
     * @param leaseSeconds The lease seconds
     * @return The created or updated subscription
     */
    fun createOrUpdateSubscription(
        channelId: String,
        topic: String,
        callbackUrl: String,
        leaseSeconds: Int
    ): Subscription {
        logger.info("Creating or updating subscription for channel: $channelId")

        val subscription = transaction {
            val existingSubscription = SubscriptionEntity.find { 
                SubscriptionsTable.channelId eq channelId 
            }.firstOrNull()

            if (existingSubscription != null) {
                logger.info("Updating existing subscription for channel: $channelId")
                existingSubscription.topic = topic
                existingSubscription.callbackUrl = callbackUrl
                existingSubscription.leaseSeconds = leaseSeconds
                existingSubscription.expiresAt = LocalDateTime.now().plusSeconds(leaseSeconds.toLong())
                existingSubscription.updatedAt = LocalDateTime.now()
                existingSubscription.status = "active"
                existingSubscription.toSubscription()
            } else {
                logger.info("Creating new subscription for channel: $channelId")
                val newSubscription = SubscriptionEntity.new {
                    this.channelId = channelId
                    this.topic = topic
                    this.callbackUrl = callbackUrl
                    this.leaseSeconds = leaseSeconds
                    this.expiresAt = LocalDateTime.now().plusSeconds(leaseSeconds.toLong())
                }
                newSubscription.toSubscription()
            }
        }

        // Update cache for this specific subscription
        cacheService.put("subscriptions", "subscription:$channelId", subscription)

        // Invalidate caches that might be affected by this change
        cacheService.remove("subscriptions", "all_active_subscriptions")

        // We can't easily clear only expiring_subscriptions cache entries with a prefix
        // So we'll just update the subscription cache and let expiring subscriptions
        // cache entries expire naturally based on their TTL

        logger.debug("Updated cache for subscription: $channelId and invalidated all_active_subscriptions cache")

        return subscription
    }

    /**
     * Get a subscription by channel ID.
     * Uses caching to improve performance.
     *
     * @param channelId The YouTube channel ID
     * @return The subscription or null if not found
     */
    fun getSubscription(channelId: String): Subscription? {
        // Try to get from cache first
        val cachedSubscription = cacheService.get<Subscription>("subscriptions", "subscription:$channelId")
        if (cachedSubscription != null) {
            logger.debug("Cache hit for subscription: $channelId")
            return cachedSubscription
        }

        // If not in cache, get from database
        logger.debug("Cache miss for subscription: $channelId, fetching from database")
        val subscription = transaction {
            SubscriptionEntity.find { 
                SubscriptionsTable.channelId eq channelId 
            }.firstOrNull()?.toSubscription()
        }

        // Store in cache if found
        if (subscription != null) {
            cacheService.put("subscriptions", "subscription:$channelId", subscription)
        }

        return subscription
    }

    /**
     * Get all active subscriptions.
     * Uses caching to improve performance.
     *
     * @return List of active subscriptions
     */
    fun getAllActiveSubscriptions(): List<Subscription> {
        // Try to get from cache first
        val cachedSubscriptions = cacheService.get<List<Subscription>>("subscriptions", "all_active_subscriptions")
        if (cachedSubscriptions != null) {
            logger.debug("Cache hit for all active subscriptions")
            return cachedSubscriptions
        }

        // If not in cache, get from database
        logger.debug("Cache miss for all active subscriptions, fetching from database")
        val subscriptions = transaction {
            SubscriptionEntity.find { 
                SubscriptionsTable.status eq "active" 
            }.map { it.toSubscription() }
        }

        // Store in cache
        cacheService.put("subscriptions", "all_active_subscriptions", subscriptions)
        logger.debug("Updated cache for all active subscriptions: ${subscriptions.size} subscriptions")

        return subscriptions
    }

    /**
     * Get subscriptions that are about to expire.
     * Uses caching to improve performance.
     *
     * @param expiryThreshold The threshold for expiry (in seconds)
     * @return List of subscriptions that will expire within the threshold
     */
    fun getExpiringSubscriptions(expiryThreshold: Long): List<Subscription> {
        val thresholdDateTime = LocalDateTime.now().plusSeconds(expiryThreshold)
        val cacheKey = "expiring_subscriptions:$expiryThreshold"

        // Try to get from cache first
        val cachedSubscriptions = cacheService.get<List<Subscription>>("subscriptions", cacheKey)
        if (cachedSubscriptions != null) {
            logger.debug("Cache hit for expiring subscriptions with threshold: $expiryThreshold")
            return cachedSubscriptions
        }

        // If not in cache, get from database
        logger.debug("Cache miss for expiring subscriptions, fetching from database")
        val subscriptions = transaction {
            SubscriptionEntity.find { 
                (SubscriptionsTable.status eq "active") and
                (SubscriptionsTable.expiresAt lessEq thresholdDateTime)
            }.map { it.toSubscription() }
        }

        // Store in cache with a shorter expiry time since this data changes frequently
        cacheService.put("subscriptions", cacheKey, subscriptions)
        logger.debug("Updated cache for expiring subscriptions: ${subscriptions.size} subscriptions")

        return subscriptions
    }

    /**
     * Update a subscription's status.
     * Updates the cache when a subscription's status is updated.
     *
     * @param channelId The YouTube channel ID
     * @param status The new status
     * @return True if the subscription was updated, false otherwise
     */
    fun updateSubscriptionStatus(channelId: String, status: String): Boolean {
        val updated = transaction {
            val subscription = SubscriptionEntity.find { 
                SubscriptionsTable.channelId eq channelId 
            }.firstOrNull()

            if (subscription != null) {
                subscription.status = status
                subscription.updatedAt = LocalDateTime.now()
                true
            } else {
                false
            }
        }

        if (updated) {
            // Get the updated subscription and update the cache
            val updatedSubscription = getSubscription(channelId)
            if (updatedSubscription != null) {
                cacheService.put("subscriptions", "subscription:$channelId", updatedSubscription)
                logger.debug("Updated cache for subscription status: $channelId -> $status")
            } else {
                // If the subscription is not found, remove it from the cache
                cacheService.remove("subscriptions", "subscription:$channelId")
                logger.debug("Removed subscription from cache: $channelId")
            }

            // Invalidate all_active_subscriptions cache since we've modified a subscription's status
            cacheService.remove("subscriptions", "all_active_subscriptions")
            logger.debug("Invalidated all_active_subscriptions cache")
        }

        return updated
    }
}
