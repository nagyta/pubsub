package com.example

import com.example.routing.configureConfigRoutes
import com.example.routing.configureHealthRoutes
import com.example.routing.configureHomeRoutes
import com.example.routing.configureNotificationRoutes
import com.example.routing.configurePubSubRoutes
import com.example.routing.configureSubscriptionRoutes
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.Routing")

/**
 * Configures all routing for the application.
 * This file is kept for backward compatibility.
 * 
 * The actual routing implementation has been refactored into separate files in the com.example.routing package.
 */
fun Application.configureRouting() {
    logger.info("Configuring application routing")

    // Configure home routes
    configureHomeRoutes()

    // Configure health check routes
    configureHealthRoutes()

    // Configure PubSubHubbub routes
    configurePubSubRoutes()

    // Configure subscription management routes
    configureSubscriptionRoutes()

    // Configure notification consumer routes
    configureNotificationRoutes()

    // Configure service configuration routes
    configureConfigRoutes()

    logger.info("All routes configured successfully")
}
