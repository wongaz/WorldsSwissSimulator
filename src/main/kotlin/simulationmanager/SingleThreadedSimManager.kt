package io.wongaz.simulationmanager

import io.wongaz.model.core.Team
import io.wongaz.simulationmanager.interfaces.AbstractSimManager

class SingleThreadedSimManager(teams: List<Team>, iterations: Int = 10000) : AbstractSimManager(teams, iterations) {
    override fun doWork() {
        TODO("Not yet implemented")
    }
}