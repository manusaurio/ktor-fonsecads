val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val sqlite3_xerial_version: String by project
val koin_version: String by project

plugins {
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.2"
                id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22"
}

group = "ar.pelotude.ktorfds"
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
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("org.xerial:sqlite-jdbc:$sqlite3_xerial_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
}