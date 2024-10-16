package io.wongaz

import io.wongaz.loader.TeamLoaderManager
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import io.wongaz.simulationmanager.sims.SingleThreadedSimManager


fun main() {
    val stream = object {}.javaClass.getResourceAsStream("/worlds2024.yml")!!
    val teams = TeamLoaderManager().getTeamsFromStream(stream)
    val simManager : AbstractSimManager = SingleThreadedSimManager(teams, 100_000)
    simManager.doWork()
    val res = simManager.getResults()
    for(r in res){
        println(r.makeSimpleResultsLine())
    }
}
