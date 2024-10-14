package io.wongaz.matchsimulation

import io.wongaz.model.Team
import kotlin.random.Random

class EquallyFavored(val randomSeed: Random): IGameSimulation{
    override fun runSingleGameSimulation(team1: Team, team2: Team): Team {
        val rng  = randomSeed.nextInt(1, 2)
        if(rng == 1) {
            return team1
        }
        return team2
    }
}