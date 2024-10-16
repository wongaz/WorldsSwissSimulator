package io.wongaz.tournamentplanner.matchmaking.rules

import io.wongaz.model.core.Team
import io.wongaz.tournamentplanner.matchmaking.graph.interfaces.ITournamentGraph
import io.wongaz.tournamentplanner.matchmaking.interfaces.AbstractMatchMakingRule
import kotlin.random.Random

/**
 * Purely Random protecting against rematches from past rounds
 * 16 Monkeys in a barrel
 */
class NoEloNoRematchRule(seed: Random):
    AbstractMatchMakingRule(seed) {

    override fun removeMatches(tournamentGraph: ITournamentGraph, teams: List<Team>) {
        for (team in teams) {
            val pastTeams = team.getPreviousPlayedTeams()
            for (pTeam in pastTeams){
                tournamentGraph.removeNode(team, pTeam)
            }
        }
    }

    override fun updateWeights(tournamentGraph: ITournamentGraph, teams: List<Team>) {
    }

    override fun unblock(tournamentGraph: ITournamentGraph, teams: List<Team>) {
    }

}