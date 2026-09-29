package io.wongaz

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.wongaz.persistence.DatabaseConfig
import io.wongaz.persistence.PostgresRunRepository
import io.wongaz.runs.DefaultRunService
import io.wongaz.server.simulatorModule

fun main() {
    val configuredPort = System.getenv("SIM_HTTP_PORT")
    val port = if (configuredPort == null) 8080 else configuredPort.toIntOrNull()
    require(port != null && port in 1..65535) { "SIM_HTTP_PORT must be an integer between 1 and 65535." }
    val service = DefaultRunService { PostgresRunRepository(DatabaseConfig.fromEnvironment()) }
    embeddedServer(Netty, host = "127.0.0.1", port = port) {
        simulatorModule(service)
    }.start(wait = true)
}
