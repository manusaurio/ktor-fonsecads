package ar.pelotude.ktorfds.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.*
import io.ktor.util.hex
import kotlin.time.Duration.Companion.days

@JvmInline
value class FrontendSession(val id: Long)

fun Application.configureSessions() {
    val devMode = (environment.config.propertyOrNull("ktor.environment")?.getString()
        ?: "prod") == "dev"

    val secretSignKey = hex(environment.config.property("ktor.signingKey").getString())

    install(Sessions) {
        cookie<FrontendSession>("session") {
            cookie.path = "/"
            cookie.maxAge = 400.days
            cookie.secure = !devMode

            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
}