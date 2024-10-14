package io.wongaz.model

import io.wongaz.matchsimulation.IGameSimulation

class MatchFactory(private val simulation: IGameSimulation) {
    fun buildMatch(t1: Team, t2: Team, ft: Int): Match{
        return Match(
            team1 = t1,
            team2 = t2,
            firstTo = ft,
            this.simulation
        )
    }
}