package com.example.fakes

import com.example.models.Subscription
import com.example.repository.interfaces.ISubscriptionRepository
import java.time.LocalDateTime

class FakeSubscriptionRepository : ISubscriptionRepository {
    private val data = mutableMapOf<String, Subscription>()

    override fun init() {}

    override fun createOrUpdateSubscription(
        channelId: String,
        topic: String,
        callbackUrl: String,
        leaseSeconds: Int
    ): Subscription {
        val sub = Subscription(
            channelId = channelId,
            topic = topic,
            callbackUrl = callbackUrl,
            leaseSeconds = leaseSeconds,
            expiresAt = LocalDateTime.now().plusSeconds(leaseSeconds.toLong())
        )
        data[channelId] = sub
        return sub
    }

    override fun getSubscription(channelId: String): Subscription? = data[channelId]

    override fun getAllActiveSubscriptions(): List<Subscription> = data.values.toList()

    override fun getExpiringSubscriptions(expiryThreshold: Long): List<Subscription> {
        val threshold = LocalDateTime.now().plusSeconds(expiryThreshold)
        return data.values.filter { it.expiresAt.isBefore(threshold) }
    }

    override fun updateSubscriptionStatus(channelId: String, status: String): Boolean {
        val sub = data[channelId] ?: return false
        data[channelId] = sub.copy(status = status, updatedAt = LocalDateTime.now())
        return true
    }

    override fun deleteSubscription(channelId: String): Boolean = data.remove(channelId) != null

    override fun getAllSubscriptions(): List<Subscription> = data.values.toList()
}
