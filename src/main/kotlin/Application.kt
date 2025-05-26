package com.example

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
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.Application")

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    // Configure XML content negotiation
    install(ContentNegotiation) {
        register(ContentType.Application.Xml, JacksonConverter(createXmlMapper()))
    }

    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Error processing request", cause)
            call.respondText(
                text = "Internal server error: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    configureRouting()
}

// Create XML mapper with Kotlin support
private fun createXmlMapper(): XmlMapper {
    val xmlModule = JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }
    return XmlMapper(xmlModule).apply {
        registerKotlinModule()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
