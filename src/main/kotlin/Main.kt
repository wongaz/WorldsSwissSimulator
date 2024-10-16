package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import io.wongaz.simulationmanager.sims.SingleThreadedSimManager
import io.wongaz.tournamentplanner.WorldsSwissFormatSchedulerComponent
import io.wongaz.tournamentplanner.create
import kotlin.random.Random

fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teams = TeamLoaderManager().getTeamsFromStream(stream)
    val simManager :AbstractSimManager = SingleThreadedSimManager(teams)
    simManager.doWork()
    val res = simManager.getResults()
    for(r in res){
        println(r.makeSimpleResultsLine())
    }
}
