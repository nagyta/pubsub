package com.example.service

import com.example.service.PubSubHubbubService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import kotlinx.coroutines.runBlocking
import org.testng.Assert
import org.testng.annotations.Test

class PubSubHubbubServiceTest {
    private fun createService(status: HttpStatusCode): PubSubHubbubService {
        val engine = MockEngine { _: HttpRequestData ->
            respond("", status, headers = Headers.Empty)
        }
        val client = HttpClient(engine)
        val service = PubSubHubbubService()
        val field = PubSubHubbubService::class.java.getDeclaredField("client")
        field.isAccessible = true
        field.set(service, client)
        return service
    }

    @Test
    fun testSendSubscriptionRequestSuccess() {
        val service = createService(HttpStatusCode.Accepted)
        runBlocking {
            val result = service.sendSubscriptionRequest("topic", "callback", 60, "subscribe")
            Assert.assertTrue(result)
        }
        service.close()
    }

    @Test
    fun testSendSubscriptionRequestFailure() {
        val service = createService(HttpStatusCode.BadRequest)
        runBlocking {
            val result = service.sendSubscriptionRequest("topic", "callback", 60, "subscribe")
            Assert.assertFalse(result)
        }
        service.close()
    }
}
