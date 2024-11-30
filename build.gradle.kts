val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("io.ktor.plugin") version "3.0.1"
}

group = "com.application"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version") // Kotlin standard library
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("io.ktor:ktor-server-cors:2.0.0")
    implementation("io.ktor:ktor-server-core:2.0.0") // Ktor Core
    implementation("io.ktor:ktor-server-netty:2.0.0") // Ktor Netty engine
    implementation("io.ktor:ktor-server-content-negotiation:2.0.0") // Ktor Content Negotiation for JSON
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("ch.qos.logback:logback-classic:$logback_version") // Logback for logging
    testImplementation("io.ktor:ktor-server-test-host:2.0.0") // Ktor Test Host for testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version") // Kotlin Test JUnit
    implementation("mysql:mysql-connector-java:8.0.28") // MySQL connector
}
