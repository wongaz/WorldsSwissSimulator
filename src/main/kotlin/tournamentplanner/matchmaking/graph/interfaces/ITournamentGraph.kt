package io.wongaz.tournamentplanner.matchmaking.graph.interfaces

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.component.Edge
import kotlin.random.Random

interface ITournamentGraph {
    fun runNodeMatching(randomSeed: Random)
    fun removeNode(team1: Team, team2: Team)
    fun getMatching(): List<Pair<Team, Team>>
}