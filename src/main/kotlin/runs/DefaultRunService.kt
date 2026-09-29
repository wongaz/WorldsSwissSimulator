package io.wongaz.runs

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.simulationmanager.sims.MultiThreadedSimManager
import java.time.Instant
import java.util.UUID

class DefaultRunService(repositoryFactory: () -> RunRepository) : RunService {
    private val repository by lazy {
        repositoryFactory().also { it.initialize() }
    }

    override val datasets = listOf(DatasetOption("worlds2024.yml", "Worlds 2024"))

    override fun listRuns(): List<RunSummary> = repository.listRuns()

    override fun getRun(id: UUID): SavedRun? = repository.findRun(id)

    override fun run(datasetId: String, iterations: Int): SavedRun {
        require(datasets.any { it.id == datasetId }) { "Choose a supported team dataset." }
        require(iterations in 1..1_000_000) { "Iterations must be between 1 and 1,000,000." }
        val stream = requireNotNull(javaClass.getResourceAsStream("/$datasetId")) {
            "The selected team dataset is missing from the application."
        }
        val teams = stream.use { TeamLoaderManager().getTeamsFromStream(it) }
        require(teams.size == 16 && teams.map { it.teamSignature }.distinct().size == 16) {
            "Worlds Swiss simulations require 16 uniquely identified teams."
        }
        val storage = repository
        val startedAt = Instant.now()
        val manager = MultiThreadedSimManager(teams, iterations)
        manager.doWork()
        val results = manager.getResults().map { result ->
            val team = result.team
            TeamRunResult(
                team.teamSignature, team.teamName, team.region, team.seed,
                team.category, team.elo, result.qualification.toInt()
            )
        }
        val run = SavedRun(
            RunSummary(UUID.randomUUID(), datasetId, iterations, startedAt, Instant.now()),
            results
        )
        storage.save(run)
        return run
    }
}
