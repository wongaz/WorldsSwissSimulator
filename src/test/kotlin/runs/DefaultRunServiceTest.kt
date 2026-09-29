package io.wongaz.runs

import io.wongaz.loader.TeamLoaderManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Timeout(30)
class DefaultRunServiceTest {
    @Test
    fun `dataset selection and invalid input never create a database repository`() {
        var factoryCalls = 0
        val service = DefaultRunService {
            factoryCalls++
            error("Invalid input must be rejected before accessing PostgreSQL.")
        }

        assertEquals(listOf(DatasetOption("worlds2024.yml", "Worlds 2024")), service.datasets)
        for (dataset in listOf("", "missing.yml", "basic_worlds.yml", "../worlds2024.yml")) {
            assertFailsWith<IllegalArgumentException> { service.run(dataset, 1) }
        }
        for (iterations in listOf(Int.MIN_VALUE, -1, 0, 1_000_001, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { service.run("worlds2024.yml", iterations) }
        }
        assertEquals(0, factoryCalls)
    }

    @Test
    fun `history and detail operations initialize one repository lazily`() {
        val repository = RecordingRepository()
        val summary = RunSummary(
            UUID.randomUUID(), "worlds2024.yml", 1,
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z")
        )
        val saved = SavedRun(summary, emptyList())
        repository.saved += saved
        var factoryCalls = 0
        val service = DefaultRunService {
            factoryCalls++
            repository
        }

        assertEquals(0, factoryCalls)
        assertEquals(listOf(summary), service.listRuns())
        assertSame(saved, service.getRun(summary.id))
        assertNull(service.getRun(UUID.randomUUID()))
        assertEquals(listOf(summary), service.listRuns())
        assertEquals(1, factoryCalls)
        assertEquals(1, repository.initializations)
        assertEquals(100, repository.lastLimit)
    }

    @Test
    fun `successful run persists complete snapshots and returns retrievable results`() {
        val repository = RecordingRepository()
        val service = DefaultRunService { repository }
        val before = Instant.now()

        val run = service.run("worlds2024.yml", 3)

        val after = Instant.now()
        assertEquals("worlds2024.yml", run.summary.datasetId)
        assertEquals(3, run.summary.iterations)
        assertNotEquals(UUID(0, 0), run.summary.id)
        assertTrue(run.summary.startedAt >= before)
        assertTrue(run.summary.completedAt >= run.summary.startedAt)
        assertTrue(run.summary.completedAt <= after)
        assertEquals(16, run.results.size)
        assertEquals(16, run.results.map { it.teamSignature }.distinct().size)
        assertEquals(24, run.results.sumOf { it.qualifications })
        val teams = assertNotNull(javaClass.getResourceAsStream("/worlds2024.yml")).use {
            TeamLoaderManager().getTeamsFromStream(it)
        }.associateBy { it.teamSignature }
        run.results.forEach { result ->
            val team = assertNotNull(teams[result.teamSignature])
            assertEquals(team.teamName, result.teamName)
            assertEquals(team.region, result.region)
            assertEquals(team.seed, result.seed)
            assertEquals(team.category, result.category)
            assertEquals(team.elo, result.elo)
            assertTrue(result.qualifications in 0..3)
            assertEquals(result.qualifications * 100.0 / 3, result.percentage(3))
        }
        assertSame(run, repository.saved.single())
        assertSame(run, service.getRun(run.summary.id))
        assertEquals(listOf(run.summary), service.listRuns())
        assertEquals(1, repository.initializations)

        val secondRun = service.run("worlds2024.yml", 1)
        assertNotEquals(run.summary.id, secondRun.summary.id)
        assertEquals(8, secondRun.results.sumOf { it.qualifications })
        assertEquals(24, run.results.sumOf { it.qualifications })
        assertEquals(2, repository.saved.size)
    }

    @Test
    fun `initialization failures propagate without saving or reporting success`() {
        val failure = SQLException("Database unavailable")
        val repository = RecordingRepository(initializationFailure = failure)
        val service = DefaultRunService { repository }

        assertSame(failure, assertFailsWith<SQLException> { service.run("worlds2024.yml", 1) })
        assertEquals(1, repository.initializations)
        assertEquals(0, repository.saveAttempts)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `save failures propagate without returning a successful run`() {
        val failure = SQLException("Transaction failed")
        val repository = RecordingRepository(saveFailure = failure)
        val service = DefaultRunService { repository }

        assertSame(failure, assertFailsWith<SQLException> { service.run("worlds2024.yml", 1) })
        assertEquals(1, repository.saveAttempts)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `history and detail failures propagate to their caller`() {
        val failure = SQLException("Connection lost")
        val repository = RecordingRepository(readFailure = failure)
        val service = DefaultRunService { repository }

        assertSame(failure, assertFailsWith<SQLException> { service.listRuns() })
        assertSame(failure, assertFailsWith<SQLException> { service.getRun(UUID.randomUUID()) })
        assertEquals(1, repository.initializations)
    }

    private class RecordingRepository(
        private val initializationFailure: SQLException? = null,
        private val saveFailure: SQLException? = null,
        private val readFailure: SQLException? = null
    ) : RunRepository {
        val saved = mutableListOf<SavedRun>()
        var initializations = 0
        var saveAttempts = 0
        var lastLimit: Int? = null

        override fun initialize() {
            initializations++
            initializationFailure?.let { throw it }
        }

        override fun save(run: SavedRun) {
            check(initializations == 1)
            saveAttempts++
            saveFailure?.let { throw it }
            saved += run
        }

        override fun listRuns(limit: Int): List<RunSummary> {
            check(initializations == 1)
            lastLimit = limit
            readFailure?.let { throw it }
            return saved.map { it.summary }.take(limit)
        }

        override fun findRun(id: UUID): SavedRun? {
            check(initializations == 1)
            readFailure?.let { throw it }
            return saved.find { it.summary.id == id }
        }
    }
}
