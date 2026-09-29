package io.wongaz.web

import io.wongaz.api.CreateRunRequest
import io.wongaz.api.DatasetDto
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TeamResultDto
import io.wongaz.api.TournamentDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RunControllerTest {
    @Test
    fun loadsOnlySelectedTournamentAndValidatesSelectorBounds() = runTest {
        val api = FakeApi().apply {
            existingRun = existingRun.copy(summary = existingRun.summary.copy(tournamentCount = 10000))
        }
        val controller = RunController(api)
        controller.openRun(api.existingRun.summary.id)
        assertEquals(RunView.SIMULATION, controller.screen.value.view)
        assertTrue(api.tournamentRequests.isEmpty())
        controller.showTournamentViewer()
        assertEquals(RunView.TOURNAMENT, controller.screen.value.view)
        assertEquals(listOf(api.existingRun.summary.id to 1), api.tournamentRequests)
        assertEquals(1, controller.screen.value.tournament?.iteration)
        controller.setTournamentNumber("10000")
        controller.openTournament()
        assertEquals(10000, controller.screen.value.tournament?.iteration)
        assertEquals(2, api.tournamentRequests.size)
        for (invalid in listOf("", "0", "-1", "10001", "1.5", "x")) {
            controller.setTournamentNumber(invalid)
            controller.openTournament()
            assertEquals("Enter a tournament number from 1 to 10000.", controller.screen.value.tournamentError)
            assertEquals(10000, controller.screen.value.tournament?.iteration)
        }
        assertEquals(2, api.tournamentRequests.size)
        controller.openTournament(9999)
        assertEquals("9999", controller.screen.value.tournamentNumberText)
        assertEquals(9999, controller.screen.value.tournament?.iteration)
        assertEquals(9999, controller.screen.value.tournamentPage)
    }

    @Test
    fun viewerKeepsItsPageWhenReturningToSimulation() = runTest {
        val api = FakeApi().apply {
            existingRun = existingRun.copy(summary = existingRun.summary.copy(tournamentCount = 10000))
        }
        val controller = RunController(api)
        controller.openRun(api.existingRun.summary.id)
        controller.showTournamentViewer()
        controller.openTournament(5000)
        controller.showSimulation()
        assertEquals(RunView.SIMULATION, controller.screen.value.view)
        assertEquals(api.existingRun, controller.screen.value.selectedRun)
        controller.showTournamentViewer()
        assertEquals(RunView.TOURNAMENT, controller.screen.value.view)
        assertEquals(5000, controller.screen.value.tournament?.iteration)
        assertEquals(2, api.tournamentRequests.size)
        controller.setTournamentNumber("9000")
        assertEquals(5000, controller.screen.value.tournamentPage)
        controller.showSimulation()
        controller.openRun(api.existingRun.summary.id)
        assertEquals(1, controller.screen.value.tournamentPage)
        assertNull(controller.screen.value.tournament)
    }

    @Test
    fun paginationIsBoundedAndIncludesEndpoints() {
        assertEquals(emptyList(), tournamentPages(1, 0))
        assertEquals(listOf(1), tournamentPages(1, 1))
        assertEquals(listOf(1, 2, 3), tournamentPages(2, 3))
        assertEquals(listOf(1, 2, 3, 4, 5, 1000000), tournamentPages(1, 1000000))
        assertEquals(listOf(1, 498, 499, 500, 501, 502, 1000000), tournamentPages(500, 1000000))
        assertEquals(
            listOf(1, 999996, 999997, 999998, 999999, 1000000),
            tournamentPages(1000000, 1000000)
        )
    }

    @Test
    fun legacyRunDoesNotFetchDetailsAndClearsPreviousTournament() = runTest {
        val api = FakeApi().apply {
            existingRun = existingRun.copy(summary = existingRun.summary.copy(tournamentCount = 10000))
        }
        val controller = RunController(api)
        controller.openRun(api.existingRun.summary.id)
        controller.showTournamentViewer()
        controller.showSimulation()
        controller.openRun(api.newRun.summary.id)
        controller.showTournamentViewer()
        assertEquals(api.newRun, controller.screen.value.selectedRun)
        assertEquals(RunView.TOURNAMENT, controller.screen.value.view)
        assertEquals(1, controller.screen.value.tournamentPage)
        assertNull(controller.screen.value.tournament)
        assertNull(controller.screen.value.tournamentError)
        assertEquals(1, api.tournamentRequests.size)
    }

    @Test
    fun detailFailureDoesNotMisreportSavedRunAndCanBeRetried() = runTest {
        val api = FakeApi().apply {
            newRun = newRun.copy(summary = newRun.summary.copy(tournamentCount = 10000))
            tournamentError = RunApiException("Details unavailable.", 503)
        }
        val controller = RunController(api)
        controller.initialize()
        controller.startRun()
        assertEquals(api.newRun, controller.screen.value.selectedRun)
        assertEquals("Simulation completed and saved to the database.", controller.screen.value.notice)
        assertNull(controller.screen.value.error)
        assertNull(controller.screen.value.tournament)
        assertTrue(api.tournamentRequests.isEmpty())
        controller.showTournamentViewer()
        assertEquals("Details unavailable.", controller.screen.value.tournamentError)
        api.tournamentError = null
        controller.openTournament()
        assertEquals(1, controller.screen.value.tournament?.iteration)
        assertNull(controller.screen.value.tournamentError)
        api.tournamentError = RunApiException("Details unavailable.", 503)
        controller.openTournament(2)
        assertNull(controller.screen.value.tournament)
        assertEquals("2", controller.screen.value.tournamentNumberText)
        assertEquals(2, controller.screen.value.tournamentPage)
        api.tournamentError = null
        controller.showSimulation()
        controller.showTournamentViewer()
        assertEquals(2, controller.screen.value.tournament?.iteration)
    }

    @Test
    fun pendingTournamentCannotMixDetailsFromAnotherRun() = runTest {
        val api = FakeApi().apply {
            existingRun = existingRun.copy(summary = existingRun.summary.copy(tournamentCount = 10000))
        }
        val controller = RunController(api)
        controller.openRun(api.existingRun.summary.id)
        controller.showTournamentViewer()
        api.tournamentGate = CompletableDeferred()
        val job = launch { controller.openTournament(2) }
        runCurrent()
        assertNull(controller.screen.value.tournament)
        assertEquals(Activity.TOURNAMENT, controller.screen.value.activity)
        controller.openRun(api.newRun.summary.id)
        controller.openTournament(3)
        controller.setTournamentNumber("4")
        controller.showSimulation()
        assertEquals(RunView.TOURNAMENT, controller.screen.value.view)
        assertEquals(api.existingRun, controller.screen.value.selectedRun)
        assertEquals("2", controller.screen.value.tournamentNumberText)
        api.tournamentGate!!.complete(Unit)
        job.join()
        assertEquals(2, controller.screen.value.tournament?.iteration)
        assertEquals(2, api.tournamentRequests.size)
        assertFalse(controller.screen.value.busy)
    }

    @Test
    fun datasetsRemainUsableWhenDatabaseIsNotConfigured() = runTest {
        val api = FakeApi().apply {
            historyError = RunApiException("Database is not configured.", 503)
        }
        val controller = RunController(api)
        controller.initialize()

        assertEquals("worlds2024", controller.screen.value.datasetId)
        assertEquals("10000", controller.screen.value.iterationsText)
        assertEquals(listOf(DatasetDto("worlds2024", "Worlds 2024")), controller.screen.value.datasets)
        assertEquals("Database is not configured.", controller.screen.value.error)
        assertFalse(controller.screen.value.busy)

        api.historyError = null
        controller.refresh()
        assertNull(controller.screen.value.error)
        assertEquals("History refreshed.", controller.screen.value.notice)
    }

    @Test
    fun validatesIterationRangeBeforeSendingAnyRun() = runTest {
        val api = FakeApi()
        val controller = RunController(api)
        controller.initialize()

        listOf("", "0", "-1", "1000001", "1.5", "1e4", "invalid", "9999999999999").forEach { invalid ->
            controller.setIterations(invalid)
            controller.startRun()
            assertEquals("Enter a whole number of iterations from 1 to 1,000,000.", controller.screen.value.error)
            assertNull(controller.screen.value.selectedRun)
            assertNull(controller.screen.value.notice)
            assertFalse(controller.screen.value.busy)
        }
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun refusesRunWithoutAvailableDataset() = runTest {
        val api = FakeApi().apply { datasetResults = emptyList() }
        val controller = RunController(api)
        controller.initialize()
        controller.selectDataset("imaginary")
        controller.startRun()

        assertEquals("Choose an available dataset.", controller.screen.value.error)
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun acceptsDefaultAndBothIterationBoundaries() = runTest {
        val api = FakeApi()
        val controller = RunController(api)
        controller.initialize()
        controller.startRun()
        controller.setIterations("1")
        controller.startRun()
        controller.setIterations(" 1000000 ")
        controller.startRun()

        assertEquals(listOf(10000, 1, 1000000), api.requests.map { it.iterations })
        assertTrue(api.requests.all { it.datasetId == "worlds2024" })
        assertEquals(api.newRun, controller.screen.value.selectedRun)
        assertEquals(listOf(api.newRun.summary), controller.screen.value.history)
        assertEquals("Simulation completed and saved to the database.", controller.screen.value.notice)
    }

    @Test
    fun failedSaveNeverReplacesSnapshotOrReportsSuccess() = runTest {
        val api = FakeApi()
        val controller = RunController(api)
        controller.initialize()
        controller.openRun(api.existingRun.summary.id)

        listOf(400, 409, 500, 503).forEach { status ->
            api.createError = RunApiException("Safe server error $status.", status)
            controller.startRun()
            assertEquals("Safe server error $status.", controller.screen.value.error)
            assertEquals(api.existingRun, controller.screen.value.selectedRun)
            assertTrue(controller.screen.value.history.isEmpty())
            assertNull(controller.screen.value.notice)
            assertFalse(controller.screen.value.busy)
        }
    }

    @Test
    fun interruptedPostWarnsAboutUnknownOutcomeAndDoesNotRetry() = runTest {
        val api = FakeApi().apply { createError = Exception("Internal network details") }
        val controller = RunController(api)
        controller.initialize()
        controller.startRun()

        val error = assertNotNull(controller.screen.value.error)
        assertTrue(error.contains("may still finish and save"))
        assertTrue(error.contains("Refresh history before trying again"))
        assertFalse(error.contains("Internal network details"))
        assertEquals(1, api.requests.size)
        assertNull(controller.screen.value.selectedRun)
        assertNull(controller.screen.value.notice)
    }

    @Test
    fun serializesStartsAndOtherActionsWhileRunIsPending() = runTest {
        val api = FakeApi().apply { createGate = CompletableDeferred() }
        val controller = RunController(api)
        controller.initialize()

        val job = launch { controller.startRun() }
        runCurrent()
        assertEquals(Activity.RUNNING, controller.screen.value.activity)
        assertNull(controller.screen.value.selectedRun)
        assertNull(controller.screen.value.notice)
        controller.startRun()
        controller.openRun(api.existingRun.summary.id)
        controller.refresh()
        controller.setIterations("20")
        controller.selectDataset("not-available")
        assertEquals(1, api.requests.size)
        assertEquals(0, api.openCalls)
        assertEquals(1, api.historyCalls)
        assertEquals(1, api.datasetCalls)
        assertEquals("10000", controller.screen.value.iterationsText)

        api.createGate!!.complete(Unit)
        job.join()
        assertEquals(api.newRun, controller.screen.value.selectedRun)
        assertFalse(controller.screen.value.busy)
    }

    @Test
    fun historySelectionAndRefreshKeepTheSelectedSnapshot() = runTest {
        val api = FakeApi().apply { historyResults = listOf(existingRun.summary) }
        val controller = RunController(api)
        controller.initialize()
        controller.openRun(api.existingRun.summary.id)
        assertEquals(api.existingRun, controller.screen.value.selectedRun)

        api.historyResults = listOf(api.newRun.summary, api.existingRun.summary)
        controller.refresh()
        assertEquals(api.historyResults, controller.screen.value.history)
        assertEquals(api.existingRun, controller.screen.value.selectedRun)
        assertEquals(1, api.openCalls)
    }

    @Test
    fun failedHistorySelectionPreservesDisplayedSnapshot() = runTest {
        val api = FakeApi()
        val controller = RunController(api)
        controller.initialize()
        controller.openRun(api.existingRun.summary.id)
        api.openError = RunApiException("Saved run not found.", 404)
        controller.openRun("missing")

        assertEquals(api.existingRun, controller.screen.value.selectedRun)
        assertEquals("Saved run not found.", controller.screen.value.error)
        assertNull(controller.screen.value.notice)
    }

    @Test
    fun inFlightSelectionCannotBeOverwrittenByAnotherAction() = runTest {
        val api = FakeApi().apply { openGate = CompletableDeferred() }
        val controller = RunController(api)
        controller.initialize()
        val job = launch { controller.openRun(api.existingRun.summary.id) }
        runCurrent()

        controller.openRun(api.newRun.summary.id)
        controller.refresh()
        controller.startRun()
        assertEquals(Activity.OPENING, controller.screen.value.activity)
        assertEquals(1, api.openCalls)
        assertEquals(1, api.historyCalls)
        assertTrue(api.requests.isEmpty())

        api.openGate!!.complete(Unit)
        job.join()
        assertEquals(api.existingRun, controller.screen.value.selectedRun)
        assertFalse(controller.screen.value.busy)
    }

    @Test
    fun cancelledActionReleasesLockWithoutReportingSuccess() = runTest {
        val api = FakeApi().apply { createGate = CompletableDeferred() }
        val controller = RunController(api)
        controller.initialize()
        val job = launch { controller.startRun() }
        runCurrent()
        job.cancel()
        job.join()

        assertFalse(controller.screen.value.busy)
        assertNull(controller.screen.value.notice)
        assertNull(controller.screen.value.selectedRun)
        controller.refresh()
        assertEquals(2, api.historyCalls)
    }

    private class FakeApi : HttpRunApi {
        var datasetResults = listOf(DatasetDto("worlds2024", "Worlds 2024"))
        var historyResults = emptyList<RunSummaryDto>()
        var historyError: Exception? = null
        var createError: Exception? = null
        var openError: Exception? = null
        var createGate: CompletableDeferred<Unit>? = null
        var openGate: CompletableDeferred<Unit>? = null
        var datasetCalls = 0
        var historyCalls = 0
        var openCalls = 0
        val requests = mutableListOf<CreateRunRequest>()
        var existingRun = snapshot("00000000-0000-0000-0000-000000000001")
        var newRun = snapshot("00000000-0000-0000-0000-000000000002")
        val tournamentRequests = mutableListOf<Pair<String, Int>>()
        var tournamentError: Exception? = null
        var tournamentGate: CompletableDeferred<Unit>? = null

        override suspend fun tournament(id: String, iteration: Int): TournamentDto {
            tournamentRequests += id to iteration
            tournamentGate?.await()
            tournamentError?.let { throw it }
            return TournamentDto(iteration, emptyList(), emptyList(), emptyList())
        }

        override suspend fun datasets(): List<DatasetDto> {
            datasetCalls++
            return datasetResults
        }

        override suspend fun history(): List<RunSummaryDto> {
            historyCalls++
            historyError?.let { throw it }
            return historyResults
        }

        override suspend fun savedRun(id: String): SavedRunDto {
            openCalls++
            openGate?.await()
            openError?.let { throw it }
            return if (id == newRun.summary.id) newRun else existingRun
        }

        override suspend fun createRun(request: CreateRunRequest): SavedRunDto {
            requests += request
            createGate?.await()
            createError?.let { throw it }
            return newRun
        }
    }
}

internal fun snapshot(id: String) = SavedRunDto(
    summary = RunSummaryDto(
        id = id,
        datasetId = "worlds2024",
        iterations = 10000,
        startedAt = "2026-09-28T07:00:00Z",
        completedAt = "2026-09-28T07:00:01Z"
    ),
    results = listOf(TeamResultDto("T1-LCK-4", "T1", "LCK", 4, "MAJOR", 1850, 7500))
)
