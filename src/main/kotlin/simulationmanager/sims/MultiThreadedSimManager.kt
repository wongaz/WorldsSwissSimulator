package io.wongaz.simulationmanager.sims

import io.wongaz.model.core.Team
import io.wongaz.simulationmanager.interfaces.AbstractSimManager

class MultiThreadedSimManager(teams : List<Team>, iterations: Int = 10_000) :
    AbstractSimManager(teams, iterations) {

        override fun doWork() {
        TODO("Not yet implemented")
    }
}