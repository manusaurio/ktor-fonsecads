package ar.pelotude

import io.ktor.server.application.*
import ar.pelotude.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureMonitoring()
    configureSecurity()
    configureSerialization()
    configureHTTP()
    configureRouting()
}
