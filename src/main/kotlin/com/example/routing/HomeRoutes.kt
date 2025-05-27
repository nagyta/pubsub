package com.example.routing

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Configures the home page route.
 */
fun Application.configureHomeRoutes() {
    routing {
        // Home page
        get("/") {
            call.respondText("YouTube PubSubHubbub Service")
        }
    }
}
