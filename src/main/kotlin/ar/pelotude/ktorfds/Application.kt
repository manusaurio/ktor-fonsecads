package ar.pelotude.ktorfds

import ar.pelotude.ktorfds.plugins.configureMonitoring
import ar.pelotude.ktorfds.plugins.configureRouting
import ar.pelotude.ktorfds.plugins.configureSerialization
import ar.pelotude.ktorfds.plugins.configureSessions
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(Koin) {
        modules(dependencies)
    }
    configureSessions()
    configureSerialization()
    configureMonitoring()
    configureRouting()
}
