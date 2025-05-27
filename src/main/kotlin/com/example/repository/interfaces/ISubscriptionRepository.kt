package com.example.repository.interfaces

import com.example.models.Subscription

/**
 * Interface for subscription repository operations.
 * This interface defines the contract for subscription repositories.
 */
interface ISubscriptionRepository {
    /**
     * Initialize the repository.
     */
    fun init()

    /**
     * Create a new subscription or update an existing one.
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
    ): Subscription

    /**
     * Get a subscription by channel ID.
     *
     * @param channelId The YouTube channel ID
     * @return The subscription or null if not found
     */
    fun getSubscription(channelId: String): Subscription?

    /**
     * Get all active subscriptions.
     *
     * @return List of active subscriptions
     */
    fun getAllActiveSubscriptions(): List<Subscription>

    /**
     * Get subscriptions that are about to expire.
     *
     * @param expiryThreshold The threshold for expiry (in seconds)
     * @return List of subscriptions that will expire within the threshold
     */
    fun getExpiringSubscriptions(expiryThreshold: Long): List<Subscription>

    /**
     * Update a subscription's status.
     *
     * @param channelId The YouTube channel ID
     * @param status The new status
     * @return True if the subscription was updated, false otherwise
     */
    fun updateSubscriptionStatus(channelId: String, status: String): Boolean

    /**
     * Delete a subscription by channel ID.
     *
     * @param channelId The YouTube channel ID
     * @return True if the subscription was deleted, false otherwise
     */
    fun deleteSubscription(channelId: String): Boolean

    /**
     * Get all subscriptions, regardless of status.
     *
     * @return List of all subscriptions
     */
    fun getAllSubscriptions(): List<Subscription>
}
