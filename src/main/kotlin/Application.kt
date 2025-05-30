package com.example

import com.example.repository.interfaces.ISubscriptionRepository
import com.example.service.interfaces.ICacheService
import com.example.service.interfaces.INotificationConsumerService
import com.example.service.interfaces.INotificationQueueService
import com.example.service.interfaces.IRateLimitService
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.Application")

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    // Log application startup
    logger.info("Starting YouTube PubSubHubbub Service - Phase 3")

    // Install Koin
    install(Koin) {
        slf4jLogger()
        modules(appModules)
    }

    // Initialize database
    initDatabase()

    // Configure content negotiation for JSON
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(createJsonMapper()))
    }

    // Configure status pages for error handling with enhanced logging
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            // Enhanced error logging with more context
            logger.error("Error processing request: ${cause.message}", cause)

            // Provide more detailed error response
            call.respondText(
                text = "Internal server error: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    // Configure rate limiting interceptor
    val rateLimitService = get<IRateLimitService>()
    intercept(ApplicationCallPipeline.Plugins) {
        if (!rateLimitService.checkRateLimit(call)) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf(
                    "error" to "Rate limit exceeded",
                    "message" to "Too many requests, please try again later"
                )
            )
            finish()
        }
    }

    // Initialize routing with logging
    logger.info("Configuring routing")
    configureRouting()
    logger.info("Application initialization complete")
}

/**
 * Initialize the database connection, repositories, and services.
 */
private fun Application.initDatabase() {
    logger.info("Initializing database connection")

    // MongoDB connection is handled in the MongoSubscriptionRepository

    // Get services from Koin
    val subscriptionRepository = get<ISubscriptionRepository>()
    val cacheService = get<ICacheService>()
    val notificationQueueService = get<INotificationQueueService>()
    val notificationConsumerService = get<INotificationConsumerService>()
    val rateLimitService = get<IRateLimitService>()

    // Initialize repositories
    subscriptionRepository.init()

    // Initialize services
    cacheService.init()
    notificationQueueService.init()
    notificationConsumerService.init()
    rateLimitService.init()
    logger.info("Rate limiting service initialized")

    // Start consuming notifications (Phase 3)
    notificationConsumerService.startConsuming()
    logger.info("Notification consumer started")

    logger.info("Database and services initialization complete")
}


// Create JSON mapper with Kotlin support and optimized configuration
private fun createJsonMapper() = jacksonObjectMapper().apply {
    // Register JavaTimeModule for Java 8 date/time types (LocalDateTime, etc.)
    registerModule(JavaTimeModule())

    // Configure date serialization
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    // Performance optimizations
    disable(SerializationFeature.INDENT_OUTPUT) // No need for pretty printing

    // Configure object mapper for faster parsing
    factory.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
    factory.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
}
