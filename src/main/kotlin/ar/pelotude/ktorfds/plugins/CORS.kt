package ar.pelotude.ktorfds.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    val devMode = (environment.config.propertyOrNull("ktor.environment")?.getString()
        ?: "prod") == "dev"

    if (devMode) return;

    install(CORS) {
        allowHost("localhost:8081")
        allowHost("localhost:8080")
        allowHeader("X-Requested-With")
        allowCredentials = true
    }
}