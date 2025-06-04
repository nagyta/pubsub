val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.3"
    id("jacoco")
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

tasks.withType<Test> {
    useTestNG()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        classDirectories.setFrom(files(classDirectories.files.map { fileTree(it) { exclude("com/example/routing/**", "com/example/repository/**", "com/example/service/**") } }))
        xml.required.set(true)
        csv.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.test {
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-config-yaml")

    // Koin for dependency injection
    implementation("io.insert-koin:koin-core:3.5.3")
    implementation("io.insert-koin:koin-ktor:3.5.3")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.3")

    // Content negotiation and XML support
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson")

    // XML parsing
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Status pages for error handling
    implementation("io.ktor:ktor-server-status-pages")

    // Client for verification (if needed)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")

    // Database for subscription persistence
    implementation("com.h2database:h2:2.2.224")
    implementation("org.jetbrains.exposed:exposed-core:0.46.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.46.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.46.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.46.0")

    // MongoDB for subscription persistence
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.1")

    // Message queue for notification queueing
    implementation("com.rabbitmq:amqp-client:5.20.0")

    // Caching
    implementation("org.ehcache:ehcache:3.10.8")
    implementation("javax.cache:cache-api:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.testng:testng:7.7.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("io.ktor:ktor-client-mock:3.1.3")

    // Testcontainers for integration testing
    testImplementation("org.testcontainers:rabbitmq:1.19.3")
    testImplementation("org.testcontainers:testcontainers:1.19.3")

    // Koin testing
    testImplementation("io.insert-koin:koin-test:3.5.3") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }
    testImplementation("io.insert-koin:koin-test-junit5:3.5.3") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
        exclude(group = "org.junit.jupiter")
    }
}
