package ar.pelotude.ktorfds

import ar.pelotude.ktorfds.plugins.configureMonitoring
import ar.pelotude.ktorfds.plugins.configureRouting
import ar.pelotude.ktorfds.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureRouting()
}
