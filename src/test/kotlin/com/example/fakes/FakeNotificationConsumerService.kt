package com.example.fakes

import com.example.models.Notification
import com.example.service.interfaces.INotificationConsumerService

class FakeNotificationConsumerService : INotificationConsumerService {
    private var running = false

    override fun init() {}

    override fun startConsuming() {
        running = true
    }

    override suspend fun processNotification(notification: Notification) {}

    override fun isRunning(): Boolean = running

    override fun stopConsuming() {
        running = false
    }
}
