package com.example.routing

import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.routing.Routing")

/**
 * Configures all routing for the application.
 * This function calls individual routing configuration functions for each functional area.
 */
fun Application.configureRouting() {
    logger.info("Configuring application routing")
    
    // Configure home routes
    configureHomeRoutes()
    logger.info("Home routes configured")
    
    // Configure health check routes
    configureHealthRoutes()
    logger.info("Health check routes configured")
    
    // Configure PubSubHubbub routes
    configurePubSubRoutes()
    logger.info("PubSubHubbub routes configured")
    
    // Configure subscription management routes
    configureSubscriptionRoutes()
    logger.info("Subscription management routes configured")
    
    // Configure notification consumer routes
    configureNotificationRoutes()
    logger.info("Notification consumer routes configured")
    
    // Configure service configuration routes
    configureConfigRoutes()
    logger.info("Service configuration routes configured")
    
    logger.info("All routes configured successfully")
}
