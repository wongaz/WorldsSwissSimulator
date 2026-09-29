package io.wongaz.server

import io.ktor.client.request.get
import io.ktor.client.plugins.defaultRequest
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.wongaz.api.ApiError
import io.wongaz.api.DatasetDto
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TournamentDto
import io.wongaz.model.core.Category
import io.wongaz.runs.DatasetOption
import io.wongaz.runs.DefaultRunService
import io.wongaz.runs.RunConfigurationException
import io.wongaz.runs.RunService
import io.wongaz.runs.RunSummary
import io.wongaz.runs.SavedRun
import io.wongaz.runs.TeamRunResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.IOException
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulatorModuleTest {
    @Test
    fun `tournament endpoint selects one iteration and rejects invalid or missing details`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        val base = "/api/runs/${service.saved.summary.id}/tournaments"
        val response = client.get("$base/2")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(service.tournament, Json.decodeFromString<TournamentDto>(response.bodyAsText()))
        for (number in listOf("0", "-1", "1.5", "x", "1000001", "9999999999999")) {
            assertEquals(HttpStatusCode.BadRequest, client.get("$base/$number").status)
        }
        for (number in listOf(1, 3, 1_000_000)) {
            assertEquals(HttpStatusCode.NotFound, client.get("$base/$number").status)
        }
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/runs/1-1-1-1-1/tournaments/1").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/runs/${UUID.randomUUID()}/tournaments/2").status)
        service.tournamentFailure = SQLException(secret)
        val failure = client.get("$base/2")
        assertEquals(HttpStatusCode.ServiceUnavailable, failure.status)
        assertFalse(failure.bodyAsText().contains(secret))
    }

    @Test
    fun `datasets work without creating a database repository`() = testApplication {
        val client = localClient()
        application {
            simulatorModule(DefaultRunService { error("Database must remain disconnected") })
        }
        val response = client.get("/api/datasets")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            listOf(DatasetDto("worlds2024.yml", "Worlds 2024")),
            Json.decodeFromString<List<DatasetDto>>(response.bodyAsText())
        )
    }

    @Test
    fun `completed run has shared JSON shape and history and detail match`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        val created = client.post("/api/runs") {
            contentType(ContentType.Application.Json)
            setBody(validRequest)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val saved = Json.decodeFromString<SavedRunDto>(created.bodyAsText())
        assertEquals(service.saved.summary.id.toString(), saved.summary.id)
        assertEquals("2026-01-01T00:00:00Z", saved.summary.startedAt)
        assertEquals("EASTERN", saved.results.single().category)
        assertEquals(2, saved.results.single().qualifications)
        assertEquals(1, service.runCalls.get())
        val history = client.get("/api/runs")
        assertEquals(listOf(saved.summary), Json.decodeFromString<List<RunSummaryDto>>(history.bodyAsText()))
        val detail = client.get("/api/runs/${saved.summary.id}")
        assertEquals(saved, Json.decodeFromString<SavedRunDto>(detail.bodyAsText()))
    }

    @Test
    fun `history endpoint caps fake service history at one hundred`() = testApplication {
        val client = localClient()
        val service = FakeService().apply {
            onList = { List(101) { saved.summary } }
        }
        application { simulatorModule(service) }
        val response = client.get("/api/runs")
        assertEquals(100, Json.decodeFromString<List<RunSummaryDto>>(response.bodyAsText()).size)
    }

    @Test
    fun `invalid dataset and iterations never invoke simulation`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        for (body in listOf(
            """{"datasetId":"unknown","iterations":2}""",
            """{"datasetId":"worlds2024.yml","iterations":0}""",
            """{"datasetId":"worlds2024.yml","iterations":-1}""",
            """{"datasetId":"worlds2024.yml","iterations":1000001}"""
        )) {
            val response = client.post("/api/runs") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(Json.decodeFromString<ApiError>(response.bodyAsText()).message.isNotBlank())
        }
        assertEquals(0, service.runCalls.get())
    }

    @Test
    fun `iteration boundaries are accepted by HTTP validation`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        for (iterations in listOf(1, 1_000_000)) {
            val response = client.post("/api/runs") {
                contentType(ContentType.Application.Json)
                setBody("""{"datasetId":"worlds2024.yml","iterations":$iterations}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
        assertEquals(2, service.runCalls.get())
    }

    @Test
    fun `malformed JSON UUID and unsupported content type return safe JSON errors`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        for (body in listOf("{", "{}", """{"datasetId":"worlds2024.yml","iterations":"two"}""")) {
            val response = client.post("/api/runs") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(ApiError("Invalid request."), Json.decodeFromString<ApiError>(response.bodyAsText()))
        }
        for (id in listOf("not-a-uuid", "1-1-1-1-1")) {
            assertEquals(HttpStatusCode.BadRequest, client.get("/api/runs/$id").status)
        }
        val wrongMedia = client.post("/api/runs") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("datasetId=worlds2024.yml&iterations=2")
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, wrongMedia.status)
        assertEquals(ApiError("Use application/json."), Json.decodeFromString<ApiError>(wrongMedia.bodyAsText()))
        assertEquals(0, service.runCalls.get())
    }

    @Test
    fun `oversized request is rejected before service invocation`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        val response = client.post("/api/runs") {
            contentType(ContentType.Application.Json)
            setBody(validRequest + " ".repeat(4096))
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals(0, service.runCalls.get())
    }

    @Test
    fun `unknown API routes and missing runs return JSON not frontend`() = testApplication {
        val client = localClient()
        application { simulatorModule(FakeService()) }
        for (path in listOf("/api", "/api/no-such-route", "/api/runs/${UUID.randomUUID()}")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(Json.decodeFromString<ApiError>(response.bodyAsText()).message.isNotBlank())
        }
    }

    @Test
    fun `database and internal failures do not disclose sensitive messages`() = testApplication {
        val client = localClient()
        val service = FakeService()
        application { simulatorModule(service) }
        for ((failure, status) in listOf(
            SQLException(secret) to HttpStatusCode.ServiceUnavailable,
            RunConfigurationException(secret) to HttpStatusCode.ServiceUnavailable,
            IllegalArgumentException(secret) to HttpStatusCode.BadRequest,
            IllegalStateException(secret) to HttpStatusCode.InternalServerError,
            IOException(secret) to HttpStatusCode.InternalServerError,
            UnsupportedOperationException(secret) to HttpStatusCode.InternalServerError
        )) {
            service.onList = { throw failure }
            val response = client.get("/api/runs")
            assertEquals(status, response.status)
            assertFalse(response.bodyAsText().contains(secret))
            val error = Json.decodeFromString<ApiError>(response.bodyAsText())
            assertTrue(error.message.isNotBlank())
            if (failure is RunConfigurationException) assertTrue(error.message.contains("SIM_DB_PASSWORD"))
        }
    }

    @Test
    fun `loopback Host and matching Origin accepted but remote and cross origin writes rejected`() = testApplication {
        val service = FakeService()
        application { simulatorModule(service) }
        for (host in listOf("localhost", "localhost:8080", "127.0.0.1:8080", "[::1]:8080")) {
            val response = client.post("/api/runs") {
                header(HttpHeaders.Host, host)
                header(HttpHeaders.Origin, "http://$host")
                contentType(ContentType.Application.Json)
                setBody(validRequest)
            }
            assertEquals(HttpStatusCode.Created, response.status, host)
        }
        for (host in listOf("evil.example", "localhost.evil.example", "127.0.0.1.evil.example")) {
            assertEquals(HttpStatusCode.Forbidden, client.get("/api/datasets") {
                header(HttpHeaders.Host, host)
            }.status)
        }
        for (origin in listOf("https://localhost:8080", "http://localhost:8081", "http://evil.example", "null")) {
            val response = client.post("/api/runs") {
                header(HttpHeaders.Host, "localhost:8080")
                header(HttpHeaders.Origin, origin)
                contentType(ContentType.Application.Json)
                setBody(validRequest)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status, origin)
        }
        val crossSite = client.post("/api/runs") {
            header(HttpHeaders.Host, "localhost:8080")
            header("Sec-Fetch-Site", "cross-site")
            contentType(ContentType.Application.Json)
            setBody(validRequest)
        }
        assertEquals(HttpStatusCode.Forbidden, crossSite.status)
        assertEquals(4, service.runCalls.get())
    }

    @Test
    fun `concurrent simulations fail fast and failure releases application permit`() = testApplication {
        val client = localClient()
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val service = FakeService().apply {
            onRun = {
                entered.complete(Unit)
                check(release.await(10, TimeUnit.SECONDS))
                throw SQLException(secret)
            }
        }
        application { simulatorModule(service) }
        coroutineScope {
            val first = async {
                client.post("/api/runs") {
                    contentType(ContentType.Application.Json)
                    setBody(validRequest)
                }
            }
            try {
                withTimeout(10_000) { entered.await() }
                val competing = withTimeout(5_000) {
                    client.post("/api/runs") {
                        contentType(ContentType.Application.Json)
                        setBody(validRequest)
                    }
                }
                assertEquals(HttpStatusCode.Conflict, competing.status)
            } finally {
                release.countDown()
            }
            assertEquals(HttpStatusCode.ServiceUnavailable, first.await().status)
            service.onRun = { service.saved }
            val next =
                client.post("/api/runs") {
                    contentType(ContentType.Application.Json)
                    setBody(validRequest)
                }
            assertEquals(HttpStatusCode.Created, next.status)
            assertEquals(2, service.runCalls.get())
        }
    }

    @Test
    fun `frontend index is served from web resources`() = testApplication {
        val client = localClient()
        application { simulatorModule(FakeService()) }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().startsWith("text/html"))
        assertTrue(response.bodyAsText().contains("<html", ignoreCase = true))
    }

    private fun ApplicationTestBuilder.localClient() = createClient {
        defaultRequest { header(HttpHeaders.Host, "localhost") }
    }

    private class FakeService : RunService {
        override val datasets = listOf(DatasetOption("worlds2024.yml", "Worlds 2024"))
        val saved = SavedRun(
            RunSummary(
                UUID.randomUUID(), "worlds2024.yml", 2,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z")
            ),
            listOf(TeamRunResult("T1", "T1", "LCK", 1, Category.EASTERN, 2100, 2))
        )
        val runCalls = AtomicInteger()
        val tournament = TournamentDto(2, emptyList(), emptyList(), emptyList())
        var tournamentFailure: SQLException? = null
        var onList: () -> List<RunSummary> = { listOf(saved.summary) }
        var onRun: () -> SavedRun = { saved }
        override fun listRuns() = onList()
        override fun getRun(id: UUID) = saved.takeIf { it.summary.id == id }
        override fun getTournament(id: UUID, iteration: Int): TournamentDto? {
            tournamentFailure?.let { throw it }
            return tournament.takeIf { id == saved.summary.id && iteration == 2 }
        }
        override fun run(datasetId: String, iterations: Int): SavedRun {
            runCalls.incrementAndGet()
            return onRun()
        }
    }

    companion object {
        private const val validRequest = """{"datasetId":"worlds2024.yml","iterations":2}"""
        private const val secret = "jdbc:postgresql://private-db/prod password=do-not-disclose"
    }
}
