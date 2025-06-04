package com.example.routing

import com.example.fakes.FakeNotificationConsumerService
import io.ktor.client.request.get
import io.ktor.server.application.*
import io.ktor.serialization.jackson.jackson
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

class NotificationRoutesTest {
    @AfterMethod
    fun tearDown() {
        if (GlobalContext.getOrNull() != null) stopKoin()
    }

    @Test
    fun testNotificationRoutes() = testApplication {
        val service = FakeNotificationConsumerService()
        application {
            install(Koin) {
                modules(module { single<com.example.service.interfaces.INotificationConsumerService> { service } })
            }
            install(ContentNegotiation) { jackson {
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        } }
            configureNotificationRoutes()
        }

        val statusBefore = client.get("/api/notifications/consumer/status")
        Assert.assertEquals(statusBefore.status, HttpStatusCode.OK)
        Assert.assertTrue(statusBefore.bodyAsText().contains("\"running\":false"))

        val start = client.post("/api/notifications/consumer/start")
        Assert.assertEquals(start.status, HttpStatusCode.OK)
        Assert.assertTrue(service.isRunning())

        val stop = client.post("/api/notifications/consumer/stop")
        Assert.assertEquals(stop.status, HttpStatusCode.OK)
        Assert.assertFalse(service.isRunning())
    }
}
