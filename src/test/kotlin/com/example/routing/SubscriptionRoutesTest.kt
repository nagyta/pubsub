package com.example.routing

import com.example.fakes.FakePubSubHubbubService
import com.example.fakes.FakeSubscriptionRepository
import io.ktor.client.request.delete
import io.ktor.server.application.*
import io.ktor.serialization.jackson.jackson
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
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

class SubscriptionRoutesTest {
    private val oldEnv = mutableMapOf<String, String?>()

    private fun setEnv(key: String, value: String) {
        val env = System.getenv()
        val cl = env.javaClass
        val field = cl.getDeclaredField("m")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(env) as MutableMap<String, String>
        oldEnv.putIfAbsent(key, map[key])
        map[key] = value
    }

    private fun restoreEnv() {
        val env = System.getenv()
        val cl = env.javaClass
        val field = cl.getDeclaredField("m")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(env) as MutableMap<String, String>
        oldEnv.forEach { (k, v) ->
            if (v == null) map.remove(k) else map[k] = v
        }
    }

    @AfterMethod
    fun tearDown() {
        if (GlobalContext.getOrNull() != null) stopKoin()
        restoreEnv()
    }

    @Test
    fun testSubscriptionLifecycle() = testApplication {
        setEnv("CALLBACK_URL", "http://callback")
        val repo = FakeSubscriptionRepository()
        val hub = FakePubSubHubbubService()
        application {
            install(Koin) {
                modules(
                    module {
                        single<com.example.repository.interfaces.ISubscriptionRepository> { repo }
                        single<com.example.service.interfaces.IPubSubHubbubService> { hub }
                    }
                )
            }
            install(ContentNegotiation) { jackson {
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        } }
            configureSubscriptionRoutes()
        }

        val createBody = """
            {"channelId":"UC1","topic":"https://t","callbackUrl":"x","leaseSeconds":60}
        """.trimIndent()
        val createResp = client.post("/api/subscriptions") {
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }
        Assert.assertEquals(createResp.status, HttpStatusCode.Created)

        val listResp = client.get("/api/subscriptions")
        Assert.assertTrue(listResp.bodyAsText().contains("UC1"))

        val getResp = client.get("/api/subscriptions/UC1")
        Assert.assertEquals(getResp.status, HttpStatusCode.OK)

        val updateBody = """
            {"channelId":"UC1","topic":"https://t","callbackUrl":"x","leaseSeconds":120}
        """.trimIndent()
        val updateResp = client.put("/api/subscriptions/UC1") {
            contentType(ContentType.Application.Json)
            setBody(updateBody)
        }
        Assert.assertEquals(updateResp.status, HttpStatusCode.OK)

        val statusBody = """{"status":"inactive"}"""
        val statusResp = client.put("/api/subscriptions/UC1/status") {
            contentType(ContentType.Application.Json)
            setBody(statusBody)
        }
        Assert.assertEquals(statusResp.status, HttpStatusCode.OK)

        val deleteResp = client.delete("/api/subscriptions/UC1")
        Assert.assertEquals(deleteResp.status, HttpStatusCode.OK)
    }
}
