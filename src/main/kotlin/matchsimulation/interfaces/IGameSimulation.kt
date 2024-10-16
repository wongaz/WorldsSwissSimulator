package io.wongaz.matchsimulation.interfaces

import io.wongaz.model.core.Team
import kotlin.random.Random

interface IGameSimulation {
    /**
     *  returns winning Team for the singleMatch
     */
    fun runSingleGameSimulation(team1: Team, team2: Team) : Team
}