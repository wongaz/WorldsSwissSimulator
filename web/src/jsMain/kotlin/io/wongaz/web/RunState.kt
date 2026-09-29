package io.wongaz.web

import io.wongaz.api.CreateRunRequest
import io.wongaz.api.DatasetDto
import io.wongaz.api.RunSummaryDto
import io.wongaz.api.SavedRunDto
import io.wongaz.api.TournamentDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

interface HttpRunApi {
    suspend fun datasets(): List<DatasetDto>
    suspend fun history(): List<RunSummaryDto>
    suspend fun savedRun(id: String): SavedRunDto
    suspend fun createRun(request: CreateRunRequest): SavedRunDto
    suspend fun tournament(id: String, iteration: Int): TournamentDto
}

class RunApiException(message: String, val status: Int? = null) : Exception(message)

enum class Activity { IDLE, LOADING, REFRESHING, OPENING, RUNNING, TOURNAMENT }

enum class RunView { SIMULATION, TOURNAMENT }

data class RunScreen(
    val datasets: List<DatasetDto> = emptyList(),
    val datasetId: String = "",
    val iterationsText: String = "10000",
    val history: List<RunSummaryDto> = emptyList(),
    val selectedRun: SavedRunDto? = null,
    val view: RunView = RunView.SIMULATION,
    val tournament: TournamentDto? = null,
    val tournamentPage: Int = 1,
    val tournamentNumberText: String = "1",
    val tournamentError: String? = null,
    val activity: Activity = Activity.IDLE,
    val error: String? = null,
    val notice: String? = null
) {
    val busy: Boolean get() = activity != Activity.IDLE
}

/** One action at a time: a slow request cannot overwrite a newer selection. */
class RunController(private val api: HttpRunApi) {
    private val mutableScreen = MutableStateFlow(RunScreen())
    val screen: StateFlow<RunScreen> = mutableScreen.asStateFlow()
    private val actions = Mutex()

    fun selectDataset(id: String) {
        if (!screen.value.busy && screen.value.datasets.any { it.id == id }) {
            mutableScreen.value = screen.value.copy(datasetId = id, error = null, notice = null)
        }
    }

    fun setIterations(value: String) {
        if (!screen.value.busy) {
            mutableScreen.value = screen.value.copy(iterationsText = value, error = null, notice = null)
        }
    }

    fun setTournamentNumber(value: String) {
        if (!screen.value.busy) {
            mutableScreen.value = screen.value.copy(tournamentNumberText = value, tournamentError = null)
        }
    }

    fun showSimulation() {
        if (!screen.value.busy) {
            mutableScreen.value = screen.value.copy(view = RunView.SIMULATION)
        }
    }

    suspend fun showTournamentViewer() = action(Activity.TOURNAMENT) {
        val run = screen.value.selectedRun ?: return@action
        mutableScreen.value = screen.value.copy(view = RunView.TOURNAMENT)
        if (run.summary.tournamentCount > 0 && screen.value.tournament == null) {
            loadTournament(run, screen.value.tournamentPage)
        }
    }

    suspend fun openTournament(number: Int? = null) = action(Activity.TOURNAMENT) {
        val run = screen.value.selectedRun ?: return@action
        val iteration = number ?: screen.value.tournamentNumberText.trim().toIntOrNull()
        if (iteration == null || iteration !in 1..run.summary.tournamentCount) {
            mutableScreen.value = screen.value.copy(
                tournamentError = "Enter a tournament number from 1 to ${run.summary.tournamentCount}."
            )
            return@action
        }
        loadTournament(run, iteration)
    }

    private fun selectRun(run: SavedRunDto) {
        mutableScreen.value = screen.value.copy(
            selectedRun = run, view = RunView.SIMULATION, tournament = null,
            tournamentPage = 1, tournamentNumberText = "1", tournamentError = null
        )
    }

    private suspend fun loadTournament(run: SavedRunDto, iteration: Int) {
        mutableScreen.value = screen.value.copy(
            view = RunView.TOURNAMENT, tournament = null, tournamentPage = iteration,
            tournamentNumberText = iteration.toString(), tournamentError = null
        )
        try {
            val tournament = api.tournament(run.summary.id, iteration)
            check(tournament.iteration == iteration) { "Unexpected tournament response." }
            mutableScreen.value = screen.value.copy(tournament = tournament)
        } catch (error: Exception) {
            error.rethrowCancellation()
            mutableScreen.value = screen.value.copy(
                tournamentError = error.safeMessage("Tournament details could not be loaded. Use View tournament to retry.")
            )
        }
    }

    suspend fun initialize() = loadLists(Activity.LOADING)

    suspend fun refresh() = loadLists(Activity.REFRESHING)

    private suspend fun loadLists(activity: Activity) = action(activity) {
        val failures = mutableListOf<String>()
        try {
            val datasets = api.datasets()
            val selected = screen.value.datasetId.takeIf { id -> datasets.any { it.id == id } }
                ?: datasets.firstOrNull()?.id.orEmpty()
            mutableScreen.value = screen.value.copy(datasets = datasets, datasetId = selected)
        } catch (error: Exception) {
            error.rethrowCancellation()
            failures += error.safeMessage("Datasets could not be loaded.")
        }
        // Dataset discovery remains available even when database setup is incomplete.
        try {
            mutableScreen.value = screen.value.copy(history = api.history().take(100))
        } catch (error: Exception) {
            error.rethrowCancellation()
            failures += error.safeMessage("Saved history could not be loaded.")
        }
        mutableScreen.value = screen.value.copy(
            error = failures.takeIf { it.isNotEmpty() }?.joinToString(" "),
            notice = if (failures.isEmpty() && activity == Activity.REFRESHING) "History refreshed." else null
        )
    }

    suspend fun openRun(id: String) = action(Activity.OPENING) {
        try {
            val run = api.savedRun(id)
            selectRun(run)
        } catch (error: Exception) {
            error.rethrowCancellation()
            mutableScreen.value = screen.value.copy(error = error.safeMessage("This saved run could not be opened."))
        }
    }

    suspend fun startRun() = action(Activity.RUNNING) {
        val current = screen.value
        val iterations = current.iterationsText.trim().toIntOrNull()
        val validation = when {
            current.datasets.none { it.id == current.datasetId } -> "Choose an available dataset."
            iterations == null || iterations !in 1..1_000_000 -> "Enter a whole number of iterations from 1 to 1,000,000."
            else -> null
        }
        if (validation != null) {
            mutableScreen.value = screen.value.copy(error = validation)
            return@action
        }
        try {
            val saved = api.createRun(CreateRunRequest(current.datasetId, iterations!!))
            mutableScreen.value = screen.value.copy(
                history = (listOf(saved.summary) + screen.value.history.filterNot { it.id == saved.summary.id }).take(100),
                notice = "Simulation completed and saved to the database."
            )
            selectRun(saved)
        } catch (error: Exception) {
            error.rethrowCancellation()
            val message = if (error is RunApiException && error.status != null) {
                error.safeMessage("The run could not be saved.")
            } else {
                "The connection was interrupted or the response could not be confirmed. " +
                    "The server may still finish and save this run. Refresh history before trying again; " +
                    "do not automatically resubmit."
            }
            mutableScreen.value = screen.value.copy(error = message)
        }
    }

    private suspend fun action(activity: Activity, block: suspend () -> Unit) {
        if (!actions.tryLock()) return
        mutableScreen.value = screen.value.copy(activity = activity, error = null, notice = null)
        try {
            block()
        } finally {
            mutableScreen.value = screen.value.copy(activity = Activity.IDLE)
            actions.unlock()
        }
    }

    private fun Exception.rethrowCancellation() {
        if (this is CancellationException) throw this
    }

    private fun Exception.safeMessage(fallback: String): String =
        (this as? RunApiException)?.message?.takeIf { it.isNotBlank() } ?: fallback
}

internal fun tournamentPages(current: Int, total: Int): List<Int> {
    if (total <= 0) return emptyList()
    val start = (current - 2).coerceAtLeast(1).coerceAtMost((total - 4).coerceAtLeast(1))
    return (listOf(1) + (start..minOf(start + 4, total)) + total).distinct().sorted()
}
