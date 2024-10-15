package io.wongaz.matchsimulation.rules

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Team
import kotlin.random.Random

class BlueSideFavored(private val seededRandom: Random = Random.Default ) : IGameSimulation {
    override fun runSingleGameSimulation(team1: Team, team2: Team): Team {
        TODO("Not yet implemented")
    }

}