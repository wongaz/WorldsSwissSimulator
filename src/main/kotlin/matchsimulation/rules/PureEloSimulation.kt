package io.wongaz.matchsimulation.rules

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Team
import me.tatarka.inject.annotations.Inject
import kotlin.math.pow
import kotlin.random.Random

@Inject
class PureEloSimulation(private val randomSeed: Random = Random.Default): IGameSimulation {
    override fun runSingleGameSimulation(team1: Team, team2: Team): Team {
        val elo1 = team1.elo
        val elo2 = team2.elo
        val difference = (elo2 - elo1)/400.0
        val probA = 1/(10.0.pow(difference) + 1)

        val rng = randomSeed.nextDouble()
        if (rng <= probA) {
            return team1
        }
        return team2
    }
}