package io.wongaz.simulationmanager.sims

import io.wongaz.model.core.Team
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import io.wongaz.tournamentplanner.WorldsSwissFormatSchedulerComponent
import io.wongaz.tournamentplanner.create
import io.wongaz.tournamentplanner.scheduler.SwissFormatScheduler
import me.tatarka.inject.annotations.Inject

@Inject
class SingleThreadedSimManager(teams: List<Team>, iterations: Int = 10_000) : AbstractSimManager(teams, iterations) {
    override fun doWork() {
        for(i in 0 until iterations) {
            val copy = super.deepCopyTeams()

            val formatScheduler = WorldsSwissFormatSchedulerComponent::class.create(copy).swissFormatScheduler
            formatScheduler.runTournament()

            val qualified = formatScheduler.getQualifiedTeams()
            for (team in qualified){
                super.updateResults(team)
            }
        }
    }
}