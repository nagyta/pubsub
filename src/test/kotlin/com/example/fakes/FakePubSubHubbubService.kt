package com.example.fakes

import com.example.service.interfaces.IPubSubHubbubService

class FakePubSubHubbubService : IPubSubHubbubService {
    var lastTopic: String? = null
    var lastCallback: String? = null
    var lastLease: Int = 0
    var lastMode: String? = null
    var respond = true

    override suspend fun sendSubscriptionRequest(
        topic: String,
        callback: String,
        leaseSeconds: Int,
        mode: String
    ): Boolean {
        lastTopic = topic
        lastCallback = callback
        lastLease = leaseSeconds
        lastMode = mode
        return respond
    }

    override fun close() {}
}
