package io.wongaz.simulationmanager.sims

import io.wongaz.model.core.Team
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import io.wongaz.tournamentplanner.WorldsSwissFormatSchedulerComponent
import io.wongaz.tournamentplanner.create
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class MultiThreadedSimManager(
    teams: List<Team>,
    iterations: Int = 10_000,
    private val workerCount: Int = Runtime.getRuntime().availableProcessors()
) : AbstractSimManager(teams, iterations) {

    override fun doWork() = runBlocking(Dispatchers.Default) {
        val workers = workerCount.coerceAtLeast(1)
        val chunkSize = (iterations + workers - 1) / workers

        val deferred = (0 until workers).map { workerIdx ->
            async {
                val start = workerIdx * chunkSize
                val end = minOf(start + chunkSize, iterations)
                val localCounts = HashMap<String, Int>()
                for (i in start until end) {
                    val copy = deepCopyTeams()
                    val scheduler = WorldsSwissFormatSchedulerComponent::class
                        .create(copy)
                        .swissFormatScheduler
                    scheduler.runTournament()
                    for (team in scheduler.getQualifiedTeams()) {
                        localCounts.merge(team.teamSignature, 1, Int::plus)
                    }
                }
                localCounts
            }
        }

        val merged = HashMap<String, Int>()
        for (workerCounts in deferred.awaitAll()) {
            for ((signature, count) in workerCounts) {
                merged.merge(signature, count, Int::plus)
            }
        }
        mergeResults(merged)
    }
}