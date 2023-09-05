package ar.pelotude.ktorfds.plugins

import ar.pelotude.ktorfds.routes.messagesRouting
import io.ktor.server.routing.routing
import io.ktor.server.application.Application

fun Application.configureRouting() {
    routing {
        messagesRouting()
    }
}
