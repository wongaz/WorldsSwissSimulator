package io.wongaz.model.core.factory

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Match
import io.wongaz.model.core.Team
import me.tatarka.inject.annotations.Inject

@Inject
class MatchFactory(private val simulation: IGameSimulation) {
    fun buildMatch(t1: Team, t2: Team, ft: Int): Match {
        return Match(
            team1 = t1,
            team2 = t2,
            firstTo = ft,
            this.simulation
        )
    }
}