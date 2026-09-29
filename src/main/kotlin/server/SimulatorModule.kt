package io.wongaz.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import io.wongaz.api.ApiError
import io.wongaz.api.CreateRunRequest
import io.wongaz.api.DatasetDto
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TeamResultDto
import io.wongaz.runs.RunConfigurationException
import io.wongaz.runs.RunService
import io.wongaz.runs.RunSummary
import io.wongaz.runs.SavedRun
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.sql.SQLException
import java.util.UUID

private const val MAX_REQUEST_BYTES = 4096

fun Application.simulatorModule(service: RunService) {
    val simulationPermit = Semaphore(1)
    val logger = environment.log
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<RunConfigurationException> { call, _ ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiError("Check SIM_DB_URL, SIM_DB_USER, and SIM_DB_PASSWORD before using run history.")
            )
        }
        exception<SQLException> { call, cause ->
            logger.warn("Database request failed ({})", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.ServiceUnavailable, ApiError("Run history is temporarily unavailable."))
        }
        exception<BadRequestException> { call, _ -> call.invalidRequest() }
        exception<ContentTransformationException> { call, _ -> call.invalidRequest() }
        exception<SerializationException> { call, _ -> call.invalidRequest() }
        exception<IllegalArgumentException> { call, _ -> call.invalidRequest() }
        exception<IllegalStateException> { call, cause ->
            if (cause is CancellationException) throw cause
            logger.error("Server request failed ({})", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.InternalServerError, ApiError("The request could not be completed."))
        }
        exception<IOException> { call, cause ->
            logger.error("Server request failed ({})", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.InternalServerError, ApiError("The request could not be completed."))
        }
        exception<Exception> { call, cause ->
            if (cause is CancellationException) throw cause
            logger.error("Server request failed ({})", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.InternalServerError, ApiError("The request could not be completed."))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ApiError("Not found."))
        }
    }
    intercept(ApplicationCallPipeline.Plugins) {
        val hosts = call.request.headers.getAll(HttpHeaders.Host)
        val authority = localAuthority(hosts?.singleOrNull())
        if (authority == null) {
            call.respond(HttpStatusCode.Forbidden, ApiError("Only local requests are allowed."))
            finish()
            return@intercept
        }
        if (call.request.local.method.value == "POST" && !sameOrigin(call, authority)) {
            call.respond(HttpStatusCode.Forbidden, ApiError("Cross-origin writes are not allowed."))
            finish()
        }
    }
    routing {
        route("/api") {
            get("/datasets") {
                val datasets = withContext(Dispatchers.IO) { service.datasets.map { DatasetDto(it.id, it.label) } }
                call.respond(datasets)
            }
            get("/runs") {
                val runs = withContext(Dispatchers.IO) { service.listRuns().take(100).map { it.toDto() } }
                call.respond(runs)
            }
            get("/runs/{id}") {
                val rawId = call.parameters["id"].orEmpty()
                val id = try {
                    UUID.fromString(rawId).also { require(it.toString().equals(rawId, ignoreCase = true)) }
                } catch (_: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Run ID must be a UUID."))
                    return@get
                }
                val run = withContext(Dispatchers.IO) { service.getRun(id)?.toDto() }
                if (run == null) {
                    call.respond(HttpStatusCode.NotFound, ApiError("Run not found."))
                } else {
                    call.respond(run)
                }
            }
            post("/runs") {
                if (call.request.contentType().withoutParameters() != ContentType.Application.Json) {
                    call.respond(HttpStatusCode.UnsupportedMediaType, ApiError("Use application/json."))
                    return@post
                }
                val body = call.boundedBody() ?: return@post
                val request = Json.decodeFromString<CreateRunRequest>(body)
                val supported = withContext(Dispatchers.IO) { service.datasets.any { it.id == request.datasetId } }
                if (!supported) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Choose a supported team dataset."))
                    return@post
                }
                if (request.iterations !in 1..1_000_000) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Iterations must be between 1 and 1,000,000."))
                    return@post
                }
                if (!simulationPermit.tryAcquire()) {
                    call.respond(HttpStatusCode.Conflict, ApiError("A simulation is already running. Try again later."))
                    return@post
                }
                val run = try {
                    withContext(Dispatchers.IO) { service.run(request.datasetId, request.iterations).toDto() }
                } finally {
                    simulationPermit.release()
                }
                call.respond(HttpStatusCode.Created, run)
            }
            route("{path...}") {
                handle { call.respond(HttpStatusCode.NotFound, ApiError("API endpoint not found.")) }
            }
        }
        staticResources("/", "web", index = "index.html")
    }
}

private suspend fun ApplicationCall.invalidRequest() {
    respond(HttpStatusCode.BadRequest, ApiError("Invalid request."))
}

private suspend fun ApplicationCall.boundedBody(): String? {
    val declaredLength = request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
    if (declaredLength != null && declaredLength > MAX_REQUEST_BYTES) {
        respond(HttpStatusCode.PayloadTooLarge, ApiError("Request body must not exceed 4096 bytes."))
        return null
    }
    val channel = receiveChannel()
    val bytes = ByteArray(MAX_REQUEST_BYTES + 1)
    var size = 0
    while (size < bytes.size) {
        val count = channel.readAvailable(bytes, size, bytes.size - size)
        if (count < 0) break
        size += count
    }
    if (size > MAX_REQUEST_BYTES) {
        respond(HttpStatusCode.PayloadTooLarge, ApiError("Request body must not exceed 4096 bytes."))
        return null
    }
    return bytes.decodeToString(0, size, throwOnInvalidSequence = true)
}

private fun localAuthority(host: String?): URI? {
    if (host == null) return null
    val uri = try {
        URI("http://$host")
    } catch (_: java.net.URISyntaxException) {
        return null
    }
    if (uri.host?.lowercase() !in setOf("localhost", "127.0.0.1", "[::1]")) return null
    if (uri.rawUserInfo != null || uri.rawPath.isNotEmpty() || uri.rawQuery != null || uri.rawFragment != null) return null
    if (uri.port != -1 && uri.port !in 1..65535) return null
    return uri
}

private fun sameOrigin(call: ApplicationCall, authority: URI): Boolean {
    if (call.request.headers["Sec-Fetch-Site"].equals("cross-site", ignoreCase = true)) return false
    val origins = call.request.headers.getAll(HttpHeaders.Origin) ?: return true
    val header = origins.singleOrNull() ?: return false
    val origin = try {
        URI(header)
    } catch (_: java.net.URISyntaxException) {
        return false
    }
    val scheme = call.request.local.scheme
    val defaultPort = if (scheme == "https") 443 else 80
    return origin.scheme == scheme && origin.host.equals(authority.host, ignoreCase = true) &&
        (if (origin.port == -1) defaultPort else origin.port) ==
        (if (authority.port == -1) defaultPort else authority.port) &&
        origin.rawUserInfo == null && origin.rawQuery == null && origin.rawFragment == null &&
        origin.rawPath.isNullOrEmpty()
}

private fun RunSummary.toDto() = RunSummaryDto(
    id.toString(), datasetId, iterations, startedAt.toString(), completedAt.toString()
)

private fun SavedRun.toDto() = SavedRunDto(summary.toDto(), results.map {
    TeamResultDto(it.teamSignature, it.teamName, it.region, it.seed, it.category.name, it.elo, it.qualifications)
})
