package io.wongaz.simulationmanager.sims

import io.wongaz.model.core.Team
import io.wongaz.simulationmanager.interfaces.AbstractSimManager
import me.tatarka.inject.annotations.Inject

@Inject
class SingleThreadedSimManager(teams: List<Team>, iterations: Int = 10000) : AbstractSimManager(teams, iterations) {
    override fun doWork() {
        TODO("Not yet implemented")
    }
}