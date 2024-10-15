package io.wongaz.matchsimulation.rules

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Team
import kotlin.random.Random

class EquallyFavored(private val randomSeed: Random): IGameSimulation {
    override fun runSingleGameSimulation(team1: Team, team2: Team): Team {
        val rng  = randomSeed.nextDouble()
        if(rng <= 0.5) {
            return team1
        }
        return team2
    }
}