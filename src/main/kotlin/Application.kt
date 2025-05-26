package com.example

import com.example.repository.SubscriptionRepository
import com.example.service.CacheService
import com.example.service.NotificationQueueService
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.Application")

// Initialize repositories and services
val cacheService = CacheService()
val subscriptionRepository = SubscriptionRepository()
val notificationQueueService = NotificationQueueService()

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    // Log application startup
    logger.info("Starting YouTube PubSubHubbub Service")

    // Initialize database
    initDatabase()

    // Configure XML content negotiation
    install(ContentNegotiation) {
        register(ContentType.Application.Xml, JacksonConverter(createXmlMapper()))
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

    // Initialize routing with logging
    logger.info("Configuring routing")
    configureRouting()
    logger.info("Application initialization complete")
}

/**
 * Initialize the database connection, repositories, and services.
 */
private fun initDatabase() {
    logger.info("Initializing database connection")

    // Connect to H2 database
    Database.connect(
        url = "jdbc:h2:file:./build/pubsub-db;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    )

    // Initialize repositories
    subscriptionRepository.init()

    // Initialize services
    cacheService.init()
    notificationQueueService.init()

    logger.info("Database and services initialization complete")
}

// Create XML mapper with Kotlin support and optimized configuration
private fun createXmlMapper(): XmlMapper {
    val xmlModule = JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }
    return XmlMapper(xmlModule).apply {
        registerKotlinModule()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        // Performance optimizations
        // Disable features that are not needed for our use case
        disable(SerializationFeature.INDENT_OUTPUT) // No need for pretty printing

        // Configure object mapper for faster parsing
        factory.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
        factory.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)

        // Enable stream optimization
        factory.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
    }
}
