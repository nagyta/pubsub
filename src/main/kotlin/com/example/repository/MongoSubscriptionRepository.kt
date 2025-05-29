package com.example.repository

import com.example.models.Subscription
import com.example.repository.interfaces.ISubscriptionRepository
import com.example.service.interfaces.ICacheService
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * MongoDB implementation of the subscription repository.
 */
class MongoSubscriptionRepository(
    private val cacheService: ICacheService,
    private val mongoConnectionString: String
) : ISubscriptionRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    private lateinit var subscriptions: MongoCollection<Subscription>

    /**
     * Initialize the MongoDB connection and create necessary indexes.
     */
    override fun init() {
        logger.info("Initializing MongoDB subscription database")
        try {
            client = MongoClient.create(mongoConnectionString)
            database = client.getDatabase("pubsub")
            subscriptions = database.getCollection<Subscription>("subscriptions")

            // Create indexes
            runBlocking<Unit> {
                // Create a unique index on channelId
                val indexOptions = com.mongodb.client.model.IndexOptions().unique(true)
                subscriptions.createIndex(Indexes.ascending("channelId"), indexOptions)
                // Create an index on status for faster queries
                subscriptions.createIndex(Indexes.ascending("status"))
                // Create an index on expiresAt for faster expiry queries
                subscriptions.createIndex(Indexes.ascending("expiresAt"))
            }
            logger.info("MongoDB subscription database initialized")
        } catch (e: Exception) {
            logger.error("Error initializing MongoDB: ${e.message}", e)
        }
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
    override fun createOrUpdateSubscription(
        channelId: String,
        topic: String,
        callbackUrl: String,
        leaseSeconds: Int
    ): Subscription {
        logger.info("Creating or updating subscription for channel: $channelId")

        try {
            // Check if subscription exists
            val existingSubscription = runBlocking<Subscription?> { 
                subscriptions.find(Filters.eq("channelId", channelId)).firstOrNull()
            }

            val subscription = if (existingSubscription != null) {
                logger.info("Updating existing subscription for channel: $channelId")
                val updatedSubscription = existingSubscription.copy(
                    topic = topic,
                    callbackUrl = callbackUrl,
                    leaseSeconds = leaseSeconds,
                    expiresAt = LocalDateTime.now().plusSeconds(leaseSeconds.toLong()),
                    updatedAt = LocalDateTime.now(),
                    status = "active"
                )

                runBlocking<Unit> {
                    subscriptions.replaceOne(
                        Filters.eq("channelId", channelId),
                        updatedSubscription,
                        ReplaceOptions().upsert(true)
                    )
                }

                updatedSubscription
            } else {
                logger.info("Creating new subscription for channel: $channelId")
                val newSubscription = Subscription(
                    channelId = channelId,
                    topic = topic,
                    callbackUrl = callbackUrl,
                    leaseSeconds = leaseSeconds,
                    expiresAt = LocalDateTime.now().plusSeconds(leaseSeconds.toLong()),
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    status = "active"
                )

                runBlocking<Unit> { 
                    subscriptions.insertOne(newSubscription)
                }

                newSubscription
            }

            // Update cache for this specific subscription
            cacheService.put("subscriptions", "subscription:$channelId", subscription)

            // Invalidate caches that might be affected by this change
            cacheService.remove("subscriptions", "all_active_subscriptions")

            logger.debug("Updated cache for subscription: $channelId and invalidated all_active_subscriptions cache")

            return subscription
        } catch (e: Exception) {
            logger.error("Error creating or updating subscription for channel $channelId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get a subscription by channel ID.
     * Uses caching to improve performance.
     *
     * @param channelId The YouTube channel ID
     * @return The subscription or null if not found
     */
    override fun getSubscription(channelId: String): Subscription? {
        try {
            // Try to get from cache first
            val cachedSubscription = cacheService.get<Subscription>("subscriptions", "subscription:$channelId")
            if (cachedSubscription != null) {
                logger.debug("Cache hit for subscription: $channelId")
                return cachedSubscription
            }

            // If not in cache, get from database
            logger.debug("Cache miss for subscription: $channelId, fetching from database")
            val subscription = runBlocking<Subscription?> { 
                subscriptions.find(Filters.eq("channelId", channelId)).firstOrNull()
            }

            // Store in cache if found
            if (subscription != null) {
                cacheService.put("subscriptions", "subscription:$channelId", subscription)
            }

            return subscription
        } catch (e: Exception) {
            logger.error("Error getting subscription for channel $channelId: ${e.message}", e)
            return null
        }
    }

    /**
     * Get all active subscriptions.
     * Uses caching to improve performance.
     *
     * @return List of active subscriptions
     */
    override fun getAllActiveSubscriptions(): List<Subscription> {
        try {
            // Try to get from cache first
            val cachedSubscriptions = cacheService.get<List<Subscription>>("subscriptions", "all_active_subscriptions")
            if (cachedSubscriptions != null) {
                logger.debug("Cache hit for all active subscriptions")
                return cachedSubscriptions
            }

            // If not in cache, get from database
            logger.debug("Cache miss for all active subscriptions, fetching from database")
            val subscriptionList = runBlocking<List<Subscription>> { 
                subscriptions.find(Filters.eq("status", "active")).toList()
            }

            // Store in cache
            cacheService.put("subscriptions", "all_active_subscriptions", subscriptionList)
            logger.debug("Updated cache for all active subscriptions: ${subscriptionList.size} subscriptions")

            return subscriptionList
        } catch (e: Exception) {
            logger.error("Error getting all active subscriptions: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Get subscriptions that are about to expire.
     * Uses caching to improve performance.
     *
     * @param expiryThreshold The threshold for expiry (in seconds)
     * @return List of subscriptions that will expire within the threshold
     */
    override fun getExpiringSubscriptions(expiryThreshold: Long): List<Subscription> {
        try {
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

            // Convert LocalDateTime to Date for MongoDB query
            val thresholdDate = Date.from(thresholdDateTime.atZone(ZoneId.systemDefault()).toInstant())

            val subscriptionList = runBlocking<List<Subscription>> {
                subscriptions.find(
                    Filters.and(
                        Filters.eq("status", "active"),
                        Filters.lte("expiresAt", thresholdDate)
                    )
                ).toList()
            }

            // Store in cache with a shorter expiry time since this data changes frequently
            cacheService.put("subscriptions", cacheKey, subscriptionList)
            logger.debug("Updated cache for expiring subscriptions: ${subscriptionList.size} subscriptions")

            return subscriptionList
        } catch (e: Exception) {
            logger.error("Error getting expiring subscriptions with threshold $expiryThreshold: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Update a subscription's status.
     * Updates the cache when a subscription's status is updated.
     *
     * @param channelId The YouTube channel ID
     * @param status The new status
     * @return True if the subscription was updated, false otherwise
     */
    override fun updateSubscriptionStatus(channelId: String, status: String): Boolean {
        try {
            val subscription = getSubscription(channelId) ?: return false

            val updatedSubscription = subscription.copy(
                status = status,
                updatedAt = LocalDateTime.now()
            )

            val result = runBlocking {
                subscriptions.replaceOne(
                    Filters.eq("channelId", channelId),
                    updatedSubscription
                )
            }

            val updated = result.modifiedCount > 0 || result.matchedCount > 0

            if (updated) {
                // Update the cache with the updated subscription
                cacheService.put("subscriptions", "subscription:$channelId", updatedSubscription)
                logger.debug("Updated cache for subscription status: $channelId -> $status")

                // Invalidate all_active_subscriptions cache since we've modified a subscription's status
                cacheService.remove("subscriptions", "all_active_subscriptions")
                logger.debug("Invalidated all_active_subscriptions cache")
            }

            return updated
        } catch (e: Exception) {
            logger.error("Error updating subscription status for channel $channelId: ${e.message}", e)
            return false
        }
    }

    /**
     * Delete a subscription by channel ID.
     * Updates the cache when a subscription is deleted.
     *
     * @param channelId The YouTube channel ID
     * @return True if the subscription was deleted, false otherwise
     */
    override fun deleteSubscription(channelId: String): Boolean {
        try {
            val result = runBlocking {
                subscriptions.deleteOne(Filters.eq("channelId", channelId))
            }

            val deleted = result.deletedCount > 0

            if (deleted) {
                // Remove the subscription from cache
                cacheService.remove("subscriptions", "subscription:$channelId")
                logger.debug("Removed subscription from cache: $channelId")

                // Invalidate all_active_subscriptions cache since we've deleted a subscription
                cacheService.remove("subscriptions", "all_active_subscriptions")
                logger.debug("Invalidated all_active_subscriptions cache")
            }

            return deleted
        } catch (e: Exception) {
            logger.error("Error deleting subscription for channel $channelId: ${e.message}", e)
            return false
        }
    }

    /**
     * Get all subscriptions, regardless of status.
     *
     * @return List of all subscriptions
     */
    override fun getAllSubscriptions(): List<Subscription> {
        try {
            return runBlocking<List<Subscription>> { 
                subscriptions.find().toList()
            }
        } catch (e: Exception) {
            logger.error("Error getting all subscriptions: ${e.message}", e)
            return emptyList()
        }
    }
}
