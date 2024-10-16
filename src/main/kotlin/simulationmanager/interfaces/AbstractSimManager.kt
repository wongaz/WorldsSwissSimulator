package io.wongaz.simulationmanager.interfaces

import io.wongaz.model.core.Team
import io.wongaz.model.simulation.SimulationResult

abstract class AbstractSimManager(val teams: List<Team>, protected val iterations: Int) {
    val simResults = teams.map { it.teamSignature to SimulationResult(it, iterations) }.toMap()

    abstract fun doWork()

    fun updateResults(team: Team) {
        this.simResults[team.teamSignature]!!.addQualification()
    }

    fun getResults(): List<SimulationResult> {
        return simResults.values.toList()
    }

    protected fun deepCopyTeams(): List<Team> {
        return this.teams.map { it.copy() }.toList()
    }
}