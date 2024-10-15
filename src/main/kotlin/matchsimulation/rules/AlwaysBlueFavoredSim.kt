package io.wongaz.matchsimulation.rules

import io.wongaz.matchsimulation.interfaces.IGameSimulation
import io.wongaz.model.core.Team

class AlwaysBlueFavoredSim(): IGameSimulation {
    override fun runSingleGameSimulation(team1: Team, team2: Team): Team {
        return team1
    }
}