package io.wongaz.matchsimulation

import io.wongaz.model.Team

interface IGameSimulation {
    /**
     *  returns winning Team for the singleMatch
     */
    fun runSingleGameSimulation(team1: Team, team2: Team) : Team
}