package com.example.fakes

import com.example.service.interfaces.IRateLimitService
import io.ktor.server.application.ApplicationCall

class FakeRateLimitService : IRateLimitService {
    private var enabled = true
    private var defaultLimit = 60
    private var apiLimit = 30
    private var pubsubLimit = 120
    private var windowSize = 60

    override fun init() {}

    override fun checkRateLimit(call: ApplicationCall): Boolean = true

    override fun getConfiguration(): Map<String, String> = mapOf(
        "enabled" to enabled.toString(),
        "defaultLimit" to defaultLimit.toString(),
        "apiLimit" to apiLimit.toString(),
        "pubsubLimit" to pubsubLimit.toString(),
        "windowSize" to windowSize.toString()
    )

    override fun updateConfiguration(
        enabled: Boolean,
        defaultLimit: Int,
        apiLimit: Int,
        pubsubLimit: Int,
        windowSize: Int
    ): Map<String, String> {
        this.enabled = enabled
        this.defaultLimit = defaultLimit
        this.apiLimit = apiLimit
        this.pubsubLimit = pubsubLimit
        this.windowSize = windowSize
        return getConfiguration()
    }
}
