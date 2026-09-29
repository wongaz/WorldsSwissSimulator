package io.wongaz.persistence

import io.wongaz.model.core.Category
import io.wongaz.runs.DefaultRunService
import io.wongaz.runs.RunRepository
import io.wongaz.runs.RunSummary
import io.wongaz.runs.SavedRun
import io.wongaz.runs.TeamRunResult
import io.wongaz.api.TournamentDto
import java.io.IOException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.ContainerLaunchException
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostgresRunRepositoryIntegrationTest {
    private lateinit var repository: RunRepository

    @BeforeEach
    fun prepareIsolatedContainerDatabase() {
        repository = PostgresRunRepository(config)
        repository.initialize()
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE TABLE tournament_results, team_run_results, simulation_runs")
            }
        }
    }

    @Test
    fun `simulation service saves a complete tournament and reloads it after restart`() {
        val service = DefaultRunService { PostgresRunRepository(config) }

        val run = service.run("worlds2024.yml", 4)
        val restartedService = DefaultRunService { PostgresRunRepository(config) }
        val reloaded = assertNotNull(restartedService.getRun(run.summary.id))

        assertEquals(run.results, reloaded.results)
        assertEquals(run.summary.id, reloaded.summary.id)
        assertEquals(run.summary.datasetId, reloaded.summary.datasetId)
        assertEquals(4, reloaded.summary.iterations)
        assertEquals(16, reloaded.results.size)
        assertEquals(32, reloaded.results.sumOf { it.qualifications })
        assertEquals(4, reloaded.summary.tournamentCount)
        val tournaments = (1..4).map { iteration ->
            val detail = assertNotNull(restartedService.getTournament(run.summary.id, iteration))
            assertEquals(service.getTournament(run.summary.id, iteration), detail)
            assertEquals(iteration, detail.iteration)
            assertEquals(33, detail.rounds.sumOf { round -> round.groups.sumOf { it.matches.size } })
            detail
        }
        val qualifications = tournaments.flatMap { it.qualified }.groupingBy { it.teamSignature }.eachCount()
        reloaded.results.forEach { assertEquals(it.qualifications, qualifications[it.teamSignature] ?: 0) }
        assertNull(restartedService.getTournament(run.summary.id, 5))
        assertNull(restartedService.getTournament(UUID.randomUUID(), 1))
        assertEquals(listOf(reloaded.summary), restartedService.listRuns())
        assertTrue(Duration.between(run.summary.startedAt, reloaded.summary.startedAt).abs() <= Duration.ofNanos(1000))
        assertTrue(Duration.between(run.summary.completedAt, reloaded.summary.completedAt).abs() <= Duration.ofNanos(1000))
    }

    @Test
    fun `complete snapshots persist across new repository instances in original result order`() {
        val run = savedRun()

        repository.save(run)
        val restartedRepository = PostgresRunRepository(config)
        restartedRepository.initialize()

        assertEquals(run, restartedRepository.findRun(run.summary.id))
        assertEquals(listOf(run.summary), restartedRepository.listRuns())
    }

    @Test
    fun `schema initialization is idempotent and preserves history`() {
        val run = savedRun()
        repository.save(run)

        repeat(3) { repository.initialize() }

        assertEquals(run, repository.findRun(run.summary.id))
        assertEquals(listOf(run.summary), repository.listRuns())
    }

    @Test
    fun `empty history and unknown run return empty and null`() {
        assertTrue(repository.listRuns().isEmpty())
        assertNull(repository.findRun(UUID.randomUUID()))
        repository.save(savedRun())
        assertNull(repository.findRun(UUID.randomUUID()))
    }

    @Test
    fun `history orders by completion time newest first and honors limit`() {
        val oldest = savedRun(completedAt = timestamp.plusSeconds(10))
        val newest = savedRun(completedAt = timestamp.plusSeconds(30))
        val middle = savedRun(completedAt = timestamp.plusSeconds(20))
        listOf(middle, newest, oldest).forEach { repository.save(it) }

        assertEquals(listOf(newest.summary, middle.summary, oldest.summary), repository.listRuns())
        assertEquals(listOf(newest.summary, middle.summary), repository.listRuns(2))
        assertEquals(listOf(newest.summary), repository.listRuns(1))
        assertEquals(3, repository.listRuns(10).size)
    }

    @Test
    fun `default history limit is one hundred and larger explicit limits are supported`() {
        val runs = (1..101).map { savedRun(completedAt = timestamp.plusSeconds(it.toLong())) }
        runs.forEach { repository.save(it) }

        assertEquals(runs.takeLast(100).asReversed().map { it.summary }, repository.listRuns())
        assertEquals(runs.asReversed().map { it.summary }, repository.listRuns(101))
    }

    @Test
    fun `nonpositive limits are rejected`() {
        for (limit in listOf(0, -1, Int.MIN_VALUE)) {
            assertFailsWith<IllegalArgumentException> { repository.listRuns(limit) }
        }
    }

    @Test
    fun `SQL injection shaped names are stored as data and leave schema intact`() {
        val run = savedRun().let {
            it.copy(results = listOf(it.results.first().copy(teamName = "'; DROP TABLE simulation_runs; --")))
        }

        repository.save(run)

        assertEquals(run, repository.findRun(run.summary.id))
        val subsequent = savedRun()
        repository.save(subsequent)
        assertEquals(subsequent, repository.findRun(subsequent.summary.id))
        assertEquals(2, repository.listRuns().size)
    }

    @Test
    fun `duplicate result signatures roll back metadata and all inserted results`() {
        val valid = savedRun()
        val invalid = valid.copy(results = valid.results + valid.results.first())

        assertFailsWith<SQLException> { repository.save(invalid) }

        assertNoRowsFor(valid.summary.id)
        repository.save(valid)
        assertEquals(valid, repository.findRun(valid.summary.id))
    }

    @Test
    fun `negative qualification count rolls back complete transaction`() {
        val valid = savedRun()
        val invalid = valid.copy(
            results = valid.results.dropLast(1) + valid.results.last().copy(qualifications = -1)
        )

        assertFailsWith<SQLException> { repository.save(invalid) }

        assertNoRowsFor(valid.summary.id)
    }

    @Test
    fun `qualifications above iteration count are rejected without leaving metadata`() {
        val valid = savedRun()
        val invalid = valid.copy(
            results = valid.results.dropLast(1) +
                valid.results.last().copy(qualifications = valid.summary.iterations + 1)
        )

        assertFailsWith<IllegalArgumentException> { repository.save(invalid) }

        assertNoRowsFor(valid.summary.id)
    }

    @Test
    fun `nonpositive iteration count is rejected by the database`() {
        for (iterations in listOf(0, -1)) {
            val invalid = savedRun().let { it.copy(summary = it.summary.copy(iterations = iterations)) }

            assertFailsWith<SQLException> { repository.save(invalid) }

            assertNoRowsFor(invalid.summary.id)
        }
    }

    @Test
    fun `duplicate run IDs do not overwrite existing snapshots`() {
        val original = savedRun()
        repository.save(original)
        val replacement = original.copy(
            results = original.results.map { it.copy(teamName = "Changed name") }
        )

        assertFailsWith<SQLException> { repository.save(replacement) }

        assertEquals(original, repository.findRun(original.summary.id))
        assertEquals(listOf(original.summary), repository.listRuns())
    }

    @Test
    fun `missing or duplicate tournaments roll back already executed batches and run metadata`() {
        val run = savedRun().let { it.copy(summary = it.summary.copy(iterations = 101, tournamentCount = 101)) }
        val firstBatch = (1..100).asSequence().map { TournamentDto(it, emptyList(), emptyList(), emptyList()) }
        assertFailsWith<IllegalArgumentException> { repository.save(run, firstBatch) }
        assertNoRowsFor(run.summary.id)
        assertFailsWith<SQLException> { repository.save(run, firstBatch + firstBatch.take(1)) }
        assertNoRowsFor(run.summary.id)
        assertFailsWith<IOException> {
            repository.save(run, sequence {
                yieldAll(firstBatch)
                throw IOException("Spool read failed")
            })
        }
        assertNoRowsFor(run.summary.id)
        val all = firstBatch + TournamentDto(101, emptyList(), emptyList(), emptyList())
        repository.save(run, all)
        assertEquals(run, repository.findRun(run.summary.id))
        assertNotNull(repository.findTournament(run.summary.id, 101))
    }

    @Test
    fun `legacy schema is upgraded without fabricating details or changing team snapshots`() {
        val run = savedRun()
        repository.save(run)
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            connection.createStatement().use {
                it.execute("DROP TABLE tournament_results")
                it.execute("ALTER TABLE simulation_runs DROP COLUMN tournament_count")
            }
        }
        repeat(2) { repository.initialize() }
        assertEquals(run, repository.findRun(run.summary.id))
        assertNull(repository.findTournament(run.summary.id, 1))
    }

    private fun assertNoRowsFor(id: UUID) {
        assertNull(repository.findRun(id))
        assertTrue(repository.listRuns().none { it.id == id })
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM team_run_results WHERE run_id = ?").use {
                it.setObject(1, id)
                it.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }
            connection.prepareStatement("SELECT COUNT(*) FROM tournament_results WHERE run_id = ?").use {
                it.setObject(1, id)
                it.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }
        }
    }

    private fun savedRun(completedAt: Instant = timestamp.plusSeconds(10)): SavedRun = SavedRun(
        summary = RunSummary(UUID.randomUUID(), "worlds2024.yml", 10, timestamp, completedAt),
        results = listOf(
            TeamRunResult("ZED", "한국팀 'Zed'", "LCK", 1, Category.EASTERN, 2100, 10),
            TeamRunResult("ALPHA", "Alpha & Company", "LEC", 3, Category.WESTERN, 1700, 4),
            TeamRunResult("MID", "Time São Paulo", "CBLOL", 2, Category.WILDCARD, 1200, 0)
        )
    )

    companion object {
        private class TestPostgres : PostgreSQLContainer<Nothing>("postgres:17-alpine")

        private val postgres = TestPostgres()
        private lateinit var config: DatabaseConfig
        private val timestamp = Instant.parse("2026-01-01T12:34:56.123456Z")

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
            "PostgreSQL integration tests require Docker. Start Docker Desktop with Linux " +
                "containers (or configure a reachable Docker daemon), then rerun integrationTest. " +
                "The postgres:17-alpine image must be available or pullable.",
            failure
        )
    }
}
