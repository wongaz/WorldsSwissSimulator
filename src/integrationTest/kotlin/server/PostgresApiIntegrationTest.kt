package io.wongaz.server

import io.ktor.client.request.get
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TournamentDto
import io.wongaz.persistence.DatabaseConfig
import io.wongaz.persistence.PostgresRunRepository
import io.wongaz.runs.DefaultRunService
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostgresApiIntegrationTest {
    @Test
    fun `POST persists complete simulation and new server reloads history and detail`() {
        var created: SavedRunDto? = null
        testApplication {
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            application { simulatorModule(DefaultRunService { PostgresRunRepository(config) }) }
            val response = client.post("/api/runs") {
                contentType(ContentType.Application.Json)
                setBody("""{"datasetId":"worlds2024.yml","iterations":4}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            created = Json.decodeFromString<SavedRunDto>(response.bodyAsText())
        }
        val saved = assertNotNull(created)
        assertEquals(16, saved.results.size)
        assertEquals(32, saved.results.sumOf { it.qualifications })
        testApplication {
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            application { simulatorModule(DefaultRunService { PostgresRunRepository(config) }) }
            val historyResponse = client.get("/api/runs")
            assertEquals(HttpStatusCode.OK, historyResponse.status)
            val history = Json.decodeFromString<List<RunSummaryDto>>(historyResponse.bodyAsText())
            val detailResponse = client.get("/api/runs/${saved.summary.id}")
            assertEquals(HttpStatusCode.OK, detailResponse.status)
            val loaded = Json.decodeFromString<SavedRunDto>(detailResponse.bodyAsText())
            assertEquals(listOf(loaded.summary), history)
            assertEquals(saved.results, loaded.results)
            assertEquals(saved.summary.id, loaded.summary.id)
            assertEquals("worlds2024.yml", loaded.summary.datasetId)
            assertEquals(4, loaded.summary.iterations)
            assertEquals(4, loaded.summary.tournamentCount)
            val tournaments = (1..4).map { iteration ->
                val response = client.get("/api/runs/${saved.summary.id}/tournaments/$iteration")
                assertEquals(HttpStatusCode.OK, response.status)
                Json.decodeFromString<TournamentDto>(response.bodyAsText()).also {
                    assertEquals(iteration, it.iteration)
                    assertEquals(listOf(1, 2, 3, 4, 5), it.rounds.map { round -> round.number })
                    assertEquals(8, it.qualified.size)
                    assertEquals(8, it.eliminated.size)
                }
            }
            val counts = tournaments.flatMap { it.qualified }.groupingBy { it.teamSignature }.eachCount()
            loaded.results.forEach { assertEquals(it.qualifications, counts[it.teamSignature] ?: 0) }
            assertEquals(
                HttpStatusCode.NotFound,
                client.get("/api/runs/${saved.summary.id}/tournaments/5").status
            )
            for ((original, reloaded) in listOf(
                saved.summary.startedAt to loaded.summary.startedAt,
                saved.summary.completedAt to loaded.summary.completedAt
            )) {
                assertTrue(
                    Duration.between(Instant.parse(original), Instant.parse(reloaded)).abs() <= Duration.ofNanos(1000)
                )
            }
        }
    }

    companion object {
        private class TestPostgres : PostgreSQLContainer<Nothing>("postgres:17-alpine")
        private val postgres = TestPostgres()
        private lateinit var config: DatabaseConfig

        @BeforeAll
        @JvmStatic
        fun startPostgres() {
            try {
                postgres.start()
            } catch (failure: ContainerLaunchException) {
                throw dockerFailure(failure)
            } catch (failure: IllegalStateException) {
                throw dockerFailure(failure)
            }
            config = DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
        }

        @AfterAll
        @JvmStatic
        fun stopPostgres() {
            postgres.stop()
        }

        private fun dockerFailure(failure: RuntimeException) = IllegalStateException(
            "PostgreSQL API integration tests require Docker. Start Docker Desktop with Linux " +
                "containers (or configure a reachable Docker daemon), then rerun integrationTest. " +
                "The postgres:17-alpine image must be available or pullable.",
            failure
        )
    }
}
