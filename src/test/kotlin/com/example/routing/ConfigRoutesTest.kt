package com.example.routing

import com.example.fakes.FakeRateLimitService
import com.example.service.MockCacheService
import io.ktor.server.application.*
import io.ktor.serialization.jackson.jackson
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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

class ConfigRoutesTest {
    @AfterMethod
    fun tearDown() {
        if (GlobalContext.getOrNull() != null) stopKoin()
    }

    @Test
    fun testGetAndUpdateConfig() = testApplication {
        application {
            install(Koin) {
                modules(
                    module {
                        single<com.example.service.interfaces.ICacheService> { MockCacheService() }
                        single<com.example.service.interfaces.IRateLimitService> { FakeRateLimitService() }
                    }
                )
            }
            install(ContentNegotiation) { jackson {
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        } }
            configureConfigRoutes()
        }

        val response = client.get("/api/config")
        Assert.assertEquals(response.status, HttpStatusCode.OK)
        Assert.assertTrue(response.bodyAsText().contains("cache"))

        val updateJson = """
            {
                "cacheEnabled": true,
                "cacheHeapSize": 50,
                "cacheTtlSeconds": 600,
                "rateLimitEnabled": false,
                "rateLimitPerMinute": 20
            }
        """.trimIndent()
        val updateResponse = client.put("/api/config") {
            contentType(ContentType.Application.Json)
            setBody(updateJson)
        }
        Assert.assertEquals(updateResponse.status, HttpStatusCode.OK)
        Assert.assertTrue(updateResponse.bodyAsText().contains("Phase 4"))
    }
}
